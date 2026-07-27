import httpx
import pytest

import app.main as main


@pytest.mark.anyio
async def test_health_endpoint_returns_service_status(monkeypatch):
    monkeypatch.setattr(main, "ai_readiness", lambda: {
        "travel_risk_yolo": "ready",
        "travel_comfort": "ready",
        "memory_embedding": "ready",
        "qdrant": "ready",
    })
    transport = httpx.ASGITransport(app=main.app)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver") as client:
        response = await client.get("/health")

    assert response.status_code == 200
    assert response.json() == {
        "code": 0,
        "message": "success",
        "data": {
            "service": "travel-mind-python-ai",
            "status": "healthy",
        },
    }


@pytest.mark.anyio
async def test_ready_checks_embedding_and_qdrant(monkeypatch):
    monkeypatch.setattr(main, "ai_readiness", lambda: {
        "travel_risk_yolo": "rule_fallback",
        "travel_comfort": "rule_fallback",
        "memory_embedding": "ready",
        "qdrant": "ready",
    })
    transport = httpx.ASGITransport(app=main.app)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver") as client:
        response = await client.get("/ready")
    assert response.status_code == 200
    assert response.json()["data"]["status"] == "ready"


@pytest.mark.anyio
async def test_ready_fails_when_qdrant_is_unavailable(monkeypatch):
    monkeypatch.setattr(main, "ai_readiness", lambda: {
        "travel_risk_yolo": "rule_fallback",
        "travel_comfort": "rule_fallback",
        "memory_embedding": "ready",
        "qdrant": "unavailable",
    })
    transport = httpx.ASGITransport(app=main.app)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver") as client:
        response = await client.get("/ready")
    assert response.status_code == 503
    assert response.json()["data"]["status"] == "unavailable"
