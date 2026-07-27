import httpx
import pytest

import app.main as main


@pytest.mark.anyio
async def test_recommendation_requires_internal_token(monkeypatch):
    monkeypatch.setenv("MEMORY_SERVICE_TOKEN", "secret-token")
    transport = httpx.ASGITransport(app=main.app)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver") as client:
        missing = await client.post("/api/recommend", json={"type": "city"})
        wrong = await client.post(
            "/api/recommend",
            json={"type": "city"},
            headers={"X-Internal-Service-Token": "wrong"},
        )

    assert missing.status_code == 401
    assert wrong.status_code == 401
