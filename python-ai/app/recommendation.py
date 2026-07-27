import os
from typing import Any, Callable
from uuid import NAMESPACE_URL, uuid5

import httpx
from pydantic import BaseModel, Field

from app.memory_knowledge import (
    EmbeddingUnavailable,
    QdrantMemoryStore,
    embed_texts,
    embedding_model_ready,
)

VECTOR_SIZE = 512
COLLECTION = "travel_destinations_v1"


class DestinationItemInput(BaseModel):
    itemId: int
    itemType: str = Field(pattern=r"^(city|attraction|hotel|restaurant)$")
    name: str
    city: str | None = None
    description: str | None = None
    tags: list[str] = Field(default_factory=list)
    rating: float = Field(default=0.0, ge=0.0, le=5.0)
    popularity: int = Field(default=0, ge=0)


class DestinationsIndexRequest(BaseModel):
    destinations: list[DestinationItemInput] = Field(default_factory=list, max_length=2000)


class RecommendRequest(BaseModel):
    budgetLevel: str | None = None
    travelStyle: str | None = None
    preferredCity: str | None = None
    preferredTags: list[str] = Field(default_factory=list)
    transportation: str | None = None
    hotelLevel: str | None = None
    dietPreference: str | None = None
    type: str = Field(default="city", pattern=r"^(city|attraction|hotel|restaurant|any)$")
    cityFilter: str | None = None
    limit: int = Field(default=10, ge=1, le=50)
    excludeIds: list[int] = Field(default_factory=list)


class RecommendResult(BaseModel):
    itemId: int
    itemType: str
    name: str
    city: str | None = None
    description: str | None = None
    tags: list[str] = Field(default_factory=list)
    rating: float = 0.0
    popularity: int = 0
    score: float = 0.0
    matchReason: str = ""


class QdrantDestinationStore(QdrantMemoryStore):
    def __init__(self, base_url: str | None = None, api_key: str | None = None):
        self.base_url = (base_url or os.getenv("QDRANT_URL", "http://localhost:6333")).rstrip("/")
        self.headers = {"api-key": api_key or os.getenv("QDRANT_API_KEY", "")} if api_key or os.getenv("QDRANT_API_KEY") else {}

    def ready(self) -> bool:
        try:
            response = httpx.get(f"{self.base_url}/collections/{COLLECTION}", headers=self.headers, timeout=3)
            if response.status_code != 200:
                return False
            vectors = response.json().get("result", {}).get("config", {}).get("params", {}).get("vectors", {})
            return vectors.get("size") == VECTOR_SIZE and str(vectors.get("distance", "")).lower() == "cosine"
        except (httpx.HTTPError, TypeError, ValueError):
            return False

    def ensure_collection(self) -> None:
        with httpx.Client(timeout=10) as client:
            response = client.get(f"{self.base_url}/collections/{COLLECTION}", headers=self.headers)
            if response.status_code == 404:
                response = client.put(
                    f"{self.base_url}/collections/{COLLECTION}",
                    headers=self.headers,
                    json={"vectors": {"size": VECTOR_SIZE, "distance": "Cosine"}},
                )
                response.raise_for_status()
                return
            response.raise_for_status()
            config = response.json().get("result", {}).get("config", {}).get("params", {}).get("vectors", {})
            if config and (config.get("size") != VECTOR_SIZE or str(config.get("distance", "")).lower() != "cosine"):
                raise RuntimeError("Qdrant collection 向量配置不匹配。")

    def upsert(self, points: list[dict[str, Any]]) -> None:
        self.ensure_collection()
        if not points:
            return
        response = httpx.put(
            f"{self.base_url}/collections/{COLLECTION}/points",
            params={"wait": "true"},
            headers=self.headers,
            json={"points": points},
            timeout=30,
        )
        response.raise_for_status()

    def search(
        self,
        vector: list[float],
        limit: int,
        item_type: str | None = None,
        city_filter: str | None = None,
        exclude_ids: list[int] | None = None,
    ) -> list[dict[str, Any]]:
        self.ensure_collection()
        filter_must: list[dict[str, Any]] = []
        if item_type and item_type != "any":
            filter_must.append({"key": "item_type", "match": {"value": item_type}})
        if city_filter:
            filter_must.append({"key": "city", "match": {"value": city_filter}})
        if exclude_ids:
            filter_must.append({"key": "item_id", "except": {"values": exclude_ids}})
        body: dict[str, Any] = {
            "vector": vector,
            "limit": min(max(limit, 1), 50),
            "with_payload": True,
            "score_threshold": 0.15,
        }
        if filter_must:
            body["filter"] = {"must": filter_must}
        response = httpx.post(
            f"{self.base_url}/collections/{COLLECTION}/points/search",
            headers=self.headers,
            json=body,
            timeout=15,
        )
        response.raise_for_status()
        return response.json().get("result") or []


def _document(item: DestinationItemInput) -> str:
    parts = [item.name, item.city or "", item.description or ""]
    parts.extend(item.tags)
    parts.append(item.itemType)
    if item.rating > 0:
        parts.append(f"评分{item.rating}")
    return "；".join(p for p in parts if p and p.strip())[:800]


def _point_id(item: DestinationItemInput) -> str:
    return str(uuid5(NAMESPACE_URL, f"dest:{item.itemType}:{item.itemId}"))


def index_destinations(
    payload: DestinationsIndexRequest,
    embedder: Callable[[list[str]], list[list[float]]] = embed_texts,
    store: QdrantDestinationStore | None = None,
) -> dict[str, Any]:
    if not payload.destinations:
        return {"indexed": 0}
    documents = [_document(item) for item in payload.destinations]
    vectors = embedder(documents)
    if len(vectors) != len(documents) or any(len(vector) != VECTOR_SIZE for vector in vectors):
        raise EmbeddingUnavailable("向量维度不正确。")
    points = [
        {
            "id": _point_id(item),
            "vector": vector,
            "payload": {
                "item_id": item.itemId,
                "item_type": item.itemType,
                "name": item.name,
                "city": item.city or "",
                "description": item.description or "",
                "tags": item.tags,
                "rating": item.rating,
                "popularity": item.popularity,
            },
        }
        for item, vector in zip(payload.destinations, vectors)
    ]
    (store or QdrantDestinationStore()).upsert(points)
    return {"indexed": len(points)}


def _preference_text(request: RecommendRequest) -> str:
    parts: list[str] = ["为用户推荐旅行目的地"]
    if request.preferredCity:
        parts.append(f"偏好城市：{request.preferredCity}")
    if request.travelStyle:
        parts.append(f"旅行风格：{request.travelStyle}")
    if request.budgetLevel:
        parts.append(f"预算级别：{request.budgetLevel}")
    if request.preferredTags:
        parts.append(f"兴趣标签：{','.join(request.preferredTags)}")
    if request.transportation:
        parts.append(f"交通偏好：{request.transportation}")
    if request.hotelLevel:
        parts.append(f"住宿偏好：{request.hotelLevel}")
    if request.dietPreference:
        parts.append(f"饮食偏好：{request.dietPreference}")
    if request.type and request.type != "any":
        parts.append(f"推荐类型：{request.type}")
    return "；".join(parts)


def recommend(
    payload: RecommendRequest,
    embedder: Callable[[list[str]], list[list[float]]] = embed_texts,
    store: QdrantDestinationStore | None = None,
) -> dict[str, Any]:
    if not embedding_model_ready():
        raise EmbeddingUnavailable("向量模型未就绪。")
    text = _preference_text(payload)
    vectors = embedder([text])
    if len(vectors) != 1 or len(vectors[0]) != VECTOR_SIZE:
        raise EmbeddingUnavailable("向量维度不正确。")
    hits = (store or QdrantDestinationStore()).search(
        vectors[0],
        payload.limit,
        item_type=payload.type,
        city_filter=payload.cityFilter,
        exclude_ids=payload.excludeIds,
    )
    results: list[dict[str, Any]] = []
    for hit in hits:
        source = hit.get("payload") or {}
        score = float(hit.get("score") or 0.0)
        rating = float(source.get("rating") or 0.0)
        popularity = int(source.get("popularity") or 0)
        # Blend vector similarity (60%) + normalized rating (20%) + normalized popularity (20%)
        blended = score * 0.6
        if rating > 0:
            blended += (rating / 5.0) * 0.2
        if popularity > 0:
            blended += min(popularity / 100.0, 1.0) * 0.2
        results.append({
            "itemId": source.get("item_id"),
            "itemType": source.get("item_type"),
            "name": source.get("name"),
            "city": source.get("city") or None,
            "description": source.get("description") or None,
            "tags": source.get("tags") or [],
            "rating": rating,
            "popularity": popularity,
            "score": round(blended, 4),
            "matchReason": f"相似度 {round(score, 2)}" + (f"，评分 {rating}" if rating > 0 else ""),
        })
    # Sort by blended score descending
    results.sort(key=lambda x: -x["score"])
    return {"results": results[: payload.limit], "type": payload.type, "fallback": len(results) == 0}


def destination_store_ready() -> bool:
    return QdrantDestinationStore().ready()
