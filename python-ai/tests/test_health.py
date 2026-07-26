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
            "readiness": "ready",
            "models": {
                "travel_risk_yolo": "ready",
                "travel_comfort": "ready",
                "memory_embedding": "ready",
                "qdrant": "ready",
            },
            "fallback_enabled": True,
        },
    }
