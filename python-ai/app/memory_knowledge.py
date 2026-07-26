import os
from functools import lru_cache
from typing import Any, Callable
from uuid import NAMESPACE_URL, uuid5

import httpx
from pydantic import BaseModel, Field


VECTOR_SIZE = 512
COLLECTION = "travel_memory_v1"
MODEL_NAME = "BAAI/bge-small-zh-v1.5"
NO_EVIDENCE = "这次旅行记录里没有找到足够证据。"


class KnowledgeItemInput(BaseModel):
    itemId: int
    itemType: str
    sourceType: str | None = None
    sourceId: int | None = None
    city: str | None = None
    placeName: str | None = None
    content: str | None = None
    aiCaption: str | None = None
    aiTags: list[str] = Field(default_factory=list, max_length=20)
    dayIndex: int | None = Field(default=None, ge=1, le=366)
    timelineEvidence: bool = False


class MemoryIndexRequest(BaseModel):
    memoryId: int
    tripId: int
    ownerScope: str = Field(min_length=32, max_length=128)
    title: str
    destinationCity: str
    items: list[KnowledgeItemInput] = Field(default_factory=list, max_length=500)


class MemoryQueryRequest(BaseModel):
    memoryId: int
    ownerScope: str = Field(min_length=32, max_length=128)
    question: str = Field(min_length=1, max_length=500)
    topK: int = Field(default=5, ge=1, le=10)


class MemoryDeleteRequest(BaseModel):
    memoryId: int
    ownerScope: str = Field(min_length=32, max_length=128)


class EmbeddingUnavailable(RuntimeError):
    pass


@lru_cache(maxsize=1)
def _model():
    try:
        from sentence_transformers import SentenceTransformer  # type: ignore

        return SentenceTransformer(os.getenv("MEMORY_EMBEDDING_MODEL", MODEL_NAME))
    except Exception as ex:
        raise EmbeddingUnavailable("中文向量模型不可用，请安装依赖并完成首次模型下载。") from ex


def embed_texts(texts: list[str]) -> list[list[float]]:
    try:
        values = _model().encode(texts, normalize_embeddings=True)
        vectors = values.tolist()
    except EmbeddingUnavailable:
        raise
    except Exception as ex:
        raise EmbeddingUnavailable("中文向量模型不可用。") from ex
    if any(len(vector) != VECTOR_SIZE for vector in vectors):
        raise EmbeddingUnavailable(f"中文向量模型维度必须为 {VECTOR_SIZE}。")
    return vectors


def embedding_model_ready() -> bool:
    try:
        return int(_model().get_sentence_embedding_dimension()) == VECTOR_SIZE
    except (EmbeddingUnavailable, TypeError, ValueError):
        return False


class QdrantMemoryStore:
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

    def replace(self, owner_scope: str, memory_id: int, points: list[dict[str, Any]]) -> None:
        self.ensure_collection()
        self.delete(owner_scope, memory_id)
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

    def search(self, owner_scope: str, memory_id: int, vector: list[float], limit: int) -> list[dict[str, Any]]:
        self.ensure_collection()
        response = httpx.post(
            f"{self.base_url}/collections/{COLLECTION}/points/search",
            headers=self.headers,
            json={
                "vector": vector,
                "limit": min(max(limit, 1), 10),
                "with_payload": True,
                "score_threshold": 0.25,
                "filter": _scope_filter(owner_scope, memory_id),
            },
            timeout=15,
        )
        response.raise_for_status()
        return response.json().get("result") or []

    def delete(self, owner_scope: str, memory_id: int) -> None:
        self.ensure_collection()
        response = httpx.post(
            f"{self.base_url}/collections/{COLLECTION}/points/delete",
            params={"wait": "true"},
            headers=self.headers,
            json={"filter": _scope_filter(owner_scope, memory_id)},
            timeout=15,
        )
        response.raise_for_status()


def index_memory(
    payload: MemoryIndexRequest,
    embedder: Callable[[list[str]], list[list[float]]] = embed_texts,
    store: QdrantMemoryStore | None = None,
) -> dict[str, Any]:
    documents = [_document(item, payload.destinationCity) for item in payload.items]
    vectors = embedder([document["text"] for document in documents]) if documents else []
    if len(vectors) != len(documents) or any(len(vector) != VECTOR_SIZE for vector in vectors):
        raise EmbeddingUnavailable("中文向量结果数量或维度不正确。")
    points = [
        {
            "id": str(uuid5(NAMESPACE_URL, f"{payload.ownerScope}:{payload.memoryId}:{item.itemId}")),
            "vector": vector,
            "payload": {
                "owner_scope": payload.ownerScope,
                "memory_id": payload.memoryId,
                "item_id": item.itemId,
                "item_type": item.itemType,
                "trip_id": payload.tripId,
                "day_index": item.dayIndex,
                "city": item.city or payload.destinationCity,
                "source_type": item.sourceType or item.itemType,
                "source_id": item.sourceId or item.itemId,
                "excerpt": document["excerpt"],
            },
        }
        for item, document, vector in zip(payload.items, documents, vectors)
    ]
    (store or QdrantMemoryStore()).replace(payload.ownerScope, payload.memoryId, points)
    return {"memoryId": payload.memoryId, "indexedItems": len(points), "embeddingModel": os.getenv("MEMORY_EMBEDDING_MODEL", MODEL_NAME)}


def query_memory(
    payload: MemoryQueryRequest,
    embedder: Callable[[list[str]], list[list[float]]] = embed_texts,
    store: QdrantMemoryStore | None = None,
) -> dict[str, Any]:
    question = payload.question.strip()
    if not question:
        raise ValueError("问题不能为空。")
    vectors = embedder([f"为这个句子生成表示以用于检索相关文章：{question}"])
    if len(vectors) != 1 or len(vectors[0]) != VECTOR_SIZE:
        raise EmbeddingUnavailable("中文向量结果维度不正确。")
    hits = (store or QdrantMemoryStore()).search(payload.ownerScope, payload.memoryId, vectors[0], payload.topK)
    citations = []
    for hit in hits:
        source = hit.get("payload") or {}
        if source.get("owner_scope") != payload.ownerScope or source.get("memory_id") != payload.memoryId:
            continue
        citations.append({
            "memoryItemId": source.get("item_id"),
            "sourceType": source.get("source_type"),
            "sourceId": source.get("source_id"),
            "excerpt": source.get("excerpt") or "旅行记录",
        })
    if not citations:
        return {"answer": NO_EVIDENCE, "citations": [], "fallback": True}
    evidence = "；".join(str(item["excerpt"]) for item in citations[:3])
    return {"answer": f"根据这次旅行记录：{evidence}", "citations": citations, "fallback": True}


def delete_memory(payload: MemoryDeleteRequest, store: QdrantMemoryStore | None = None) -> dict[str, Any]:
    (store or QdrantMemoryStore()).delete(payload.ownerScope, payload.memoryId)
    return {"memoryId": payload.memoryId, "deleted": True}


def _document(item: KnowledgeItemInput, destination_city: str) -> dict[str, str]:
    bits = [f"第 {item.dayIndex} 天" if item.dayIndex else "", item.city or destination_city, item.placeName or ""]
    bits.extend([item.aiCaption or "", "、".join(item.aiTags), item.content or ""])
    if item.timelineEvidence:
        bits.append("已纳入旅行时间线")
    text = "；".join(bit.strip() for bit in bits if bit and bit.strip())[:1200]
    # Qdrant 只留引用所需的短摘要；完整消费备注仅参与本次向量计算，不持久化到向量元数据。
    excerpt_bits = [f"第 {item.dayIndex} 天" if item.dayIndex else "", item.placeName or "", item.aiCaption or item.content or ""]
    excerpt = "，".join(bit.strip() for bit in excerpt_bits if bit and bit.strip())[:240] or "旅行记录"
    return {"text": text or f"{destination_city}旅行记录", "excerpt": excerpt}


def _scope_filter(owner_scope: str, memory_id: int) -> dict[str, Any]:
    return {"must": [
        {"key": "owner_scope", "match": {"value": owner_scope}},
        {"key": "memory_id", "match": {"value": memory_id}},
    ]}
