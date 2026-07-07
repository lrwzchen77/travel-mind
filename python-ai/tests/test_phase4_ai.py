import httpx
import pytest

from app.main import app


@pytest.mark.anyio
async def test_vision_detect_accepts_image_url_and_returns_stable_labels():
    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver") as client:
        response = await client.post(
            "/api/vision/detect",
            json={
                "image_url": "https://example.com/west-lake-night-food.jpg",
                "city": "Hangzhou",
                "resource_type": "attraction",
            },
        )

    assert response.status_code == 200
    body = response.json()
    assert body["code"] == 0
    assert body["data"]["model_mode"] == "rule"
    assert body["data"]["labels"][0]["name"] == "travel_scene"
    assert "night_view" in body["data"]["scene_tags"]
    assert "Hangzhou" in body["data"]["summary"]


@pytest.mark.anyio
async def test_vision_detect_accepts_uploaded_image_file():
    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver") as client:
        response = await client.post(
            "/api/vision/detect",
            data={"city": "Hangzhou", "resource_type": "hotel"},
            files={"file": ("hotel-room.jpg", b"fake-image-bytes", "image/jpeg")},
        )

    assert response.status_code == 200
    data = response.json()["data"]
    assert data["source"] == "upload"
    assert "hotel" in data["scene_tags"]


@pytest.mark.anyio
async def test_trip_evaluate_scores_dense_days_as_medium_or_high_risk():
    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver") as client:
        response = await client.post(
            "/api/trip/evaluate",
            json={
                "transportation": "公共交通",
                "budget": 1200,
                "preferences": ["轻松"],
                "days": [
                    {
                        "date": "2026-08-01",
                        "city": "Hangzhou",
                        "attractions": ["西湖", "灵隐寺", "宋城", "河坊街"],
                        "weather": "rain",
                    }
                ],
            },
        )

    assert response.status_code == 200
    data = response.json()["data"]
    assert data["comfort_score"] < 80
    assert data["risk_level"] in {"medium", "high"}
    assert data["daily_risks"][0]["risk_items"]
    assert data["suggestions"]


@pytest.mark.anyio
async def test_content_analyze_extracts_sentiment_keywords_and_warnings():
    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver") as client:
        response = await client.post(
            "/api/content/analyze",
            json={
                "text": "西湖风景很好，适合亲子散步，但是节假日排队很久，人很多。",
                "city": "杭州",
                "attraction_name": "西湖",
                "language": "zh",
            },
        )

    assert response.status_code == 200
    data = response.json()["data"]
    assert data["sentiment"] == "mixed"
    assert "西湖" in data["keywords"]
    assert data["positive_highlights"]
    assert data["negative_warnings"]
    assert "family" in data["suitable_traveler_types"]
