import httpx
import pytest

from app.main import app


@pytest.mark.anyio
async def test_health_endpoint_returns_service_status():
    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver") as client:
        response = await client.get("/health")

    assert response.status_code == 200
    assert response.json() == {
        "code": 0,
        "message": "success",
        "data": {
            "service": "travel-mind-python-ai",
            "status": "healthy",
            "mode": "phase-1",
        },
    }
