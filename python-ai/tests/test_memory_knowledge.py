import httpx
import pytest

import app.main as main
import app.memory_knowledge as knowledge


def _vectors(texts: list[str]) -> list[list[float]]:
    return [[1.0] + [0.0] * (knowledge.VECTOR_SIZE - 1) for _ in texts]


def _index_payload(scope: str = "a" * 64) -> dict:
    return {
        "memoryId": 301,
        "tripId": 901,
        "ownerScope": scope,
        "title": "杭州旅行",
        "destinationCity": "杭州",
        "items": [{
            "itemId": 11,
            "itemType": "place",
            "sourceType": "trip_item",
            "sourceId": 81,
            "city": "杭州",
            "placeName": "西湖",
            "content": "湖边慢游",
            "aiCaption": "上午游览西湖",
            "aiTags": ["湖景"],
            "dayIndex": 1,
            "timelineEvidence": True,
        }],
    }


class Store:
    def __init__(self):
        self.points = []
        self.replaces = 0
        self.deleted = []
        self.search_args = None

    def replace(self, owner_scope, memory_id, points):
        self.replaces += 1
        self.points = points

    def search(self, owner_scope, memory_id, vector, limit):
        self.search_args = (owner_scope, memory_id, limit)
        return [{"score": 0.9, "payload": self.points[0]["payload"]}] if self.points else []

    def delete(self, owner_scope, memory_id):
        self.deleted.append((owner_scope, memory_id))
        self.points = []


def test_document_construction_and_idempotent_index_keep_only_safe_metadata():
    store = Store()
    payload = knowledge.MemoryIndexRequest.model_validate(_index_payload())

    first = knowledge.index_memory(payload, _vectors, store)
    point_id = store.points[0]["id"]
    knowledge.index_memory(payload, _vectors, store)

    assert first["indexedItems"] == 1
    assert store.replaces == 2
    assert store.points[0]["id"] == point_id
    assert store.points[0]["payload"] == {
        "owner_scope": "a" * 64,
        "memory_id": 301,
        "item_id": 11,
        "item_type": "place",
        "trip_id": 901,
        "day_index": 1,
        "city": "杭州",
        "source_type": "trip_item",
        "source_id": 81,
        "excerpt": "第 1 天，西湖，上午游览西湖",
    }


def test_query_forces_owner_and_memory_filter_and_returns_citations():
    store = Store()
    index = knowledge.MemoryIndexRequest.model_validate(_index_payload())
    knowledge.index_memory(index, _vectors, store)
    result = knowledge.query_memory(knowledge.MemoryQueryRequest(
        memoryId=301, ownerScope="a" * 64, question="去了哪里？", topK=99 if False else 5
    ), _vectors, store)

    assert store.search_args == ("a" * 64, 301, 5)
    assert result["fallback"] is True
    assert result["citations"] == [{
        "memoryItemId": 11,
        "sourceType": "trip_item",
        "sourceId": 81,
        "excerpt": "第 1 天，西湖，上午游览西湖",
    }]
    assert "西湖" in result["answer"]
    assert knowledge._scope_filter("a" * 64, 301)["must"] == [
        {"key": "owner_scope", "match": {"value": "a" * 64}},
        {"key": "memory_id", "match": {"value": 301}},
    ]


def test_query_empty_evidence_and_delete_are_explicit():
    store = Store()
    request = knowledge.MemoryQueryRequest(memoryId=301, ownerScope="a" * 64, question="哪家餐厅？")

    result = knowledge.query_memory(request, _vectors, store)
    deleted = knowledge.delete_memory(knowledge.MemoryDeleteRequest(memoryId=301, ownerScope="a" * 64), store)

    assert result == {"answer": knowledge.NO_EVIDENCE, "citations": [], "fallback": True}
    assert deleted["deleted"] is True
    assert store.deleted == [("a" * 64, 301)]


def test_embedding_unavailable_is_not_replaced_with_fake_vectors(monkeypatch):
    knowledge._model.cache_clear()
    monkeypatch.setattr(knowledge, "_model", lambda: (_ for _ in ()).throw(RuntimeError("offline")))
    with pytest.raises(knowledge.EmbeddingUnavailable, match="不可用"):
        knowledge.embed_texts(["杭州"])


@pytest.mark.anyio
async def test_memory_api_requires_internal_token(monkeypatch):
    monkeypatch.setenv("MEMORY_SERVICE_TOKEN", "secret-token")
    transport = httpx.ASGITransport(app=main.app)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver") as client:
        missing = await client.post("/api/memory/index", json=_index_payload())
        wrong = await client.post("/api/memory/index", json=_index_payload(), headers={"X-Internal-Service-Token": "wrong"})

    assert missing.status_code == 401
    assert wrong.status_code == 401


@pytest.mark.anyio
async def test_memory_api_reports_embedding_unavailable(monkeypatch):
    monkeypatch.setenv("MEMORY_SERVICE_TOKEN", "secret-token")
    monkeypatch.setattr(main, "index_memory", lambda _: (_ for _ in ()).throw(knowledge.EmbeddingUnavailable("unavailable")))
    transport = httpx.ASGITransport(app=main.app)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver") as client:
        response = await client.post(
            "/api/memory/index", json=_index_payload(), headers={"X-Internal-Service-Token": "secret-token"}
        )

    assert response.status_code == 503
    assert response.json()["detail"] == "unavailable"
