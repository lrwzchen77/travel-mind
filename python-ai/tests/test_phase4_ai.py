import httpx
import pytest
import base64
from pathlib import Path

import app.main as main
from app.main import app

AUTH = {"X-Internal-Service-Token": "travelmind-dev-memory-token-change-me"}


@pytest.mark.anyio
async def test_vision_detect_accepts_data_url_and_returns_stable_labels():
    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver", headers=AUTH) as client:
        response = await client.post(
            "/api/vision/detect",
            json={
                "image_url": "data:image/jpeg;base64," + base64.b64encode(b"not-a-real-image").decode(),
                "city": "Hangzhou",
                "resource_type": "attraction",
            },
        )

    assert response.status_code == 200
    body = response.json()
    assert body["code"] == 0
    assert body["data"]["model_mode"] == "rule"
    assert body["data"]["labels"][0]["name"] == "travel_scene"
    assert "attraction" in body["data"]["scene_tags"]
    assert "Hangzhou" in body["data"]["summary"]


@pytest.mark.anyio
async def test_vision_detect_accepts_uploaded_image_file():
    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver", headers=AUTH) as client:
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
async def test_vision_detect_materializes_browser_data_url(monkeypatch):
    image_bytes = b"browser-upload-image"

    def fake_detection(image_source):
        path = Path(image_source)
        assert path.read_bytes() == image_bytes
        return {
            "model_mode": "trained_yolo",
            "labels": [{"name": "scenic_spot", "confidence": 0.94}],
            "scene_tags": ["scenic_spot"],
            "risk_hints": [],
        }

    monkeypatch.setattr(main, "_try_yolo_detection", fake_detection)
    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver", headers=AUTH) as client:
        response = await client.post(
            "/api/vision/detect",
            json={
                "image_url": "data:image/jpeg;base64," + base64.b64encode(image_bytes).decode(),
                "city": "杭州",
                "resource_type": "travel_scene",
            },
        )

    assert response.status_code == 200
    data = response.json()["data"]
    assert data["model_mode"] == "trained_yolo"
    assert data["source"] == "upload"
    assert data["labels"][0]["name"] == "scenic_spot"


@pytest.mark.anyio
async def test_trip_evaluate_scores_dense_days_as_medium_or_high_risk():
    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver", headers=AUTH) as client:
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
    assert data["model_mode"] == "trained_travel_comfort"
    assert data["model_version"] == "travel-comfort-v1"
    assert data["comfort_score"] < 80
    assert data["risk_level"] in {"medium", "high"}
    assert data["daily_risks"][0]["risk_items"]
    assert data["suggestions"]


@pytest.mark.anyio
async def test_content_analyze_extracts_sentiment_keywords_and_warnings():
    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver", headers=AUTH) as client:
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


@pytest.mark.anyio
async def test_trained_yolo_model_returns_non_rule_result(monkeypatch):
    weights = Path(__file__).resolve().parents[1] / "models" / "travel-risk-yolo-best.pt"
    image = Path(__file__).resolve().parent / "fixtures" / "crowded_scene.jpg"
    monkeypatch.setenv("TRAVEL_MIND_YOLO_MODEL", str(weights))
    main._YOLO_MODEL = None
    main._YOLO_MODEL_PATH = None

    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver", headers=AUTH) as client:
        response = await client.post(
            "/api/vision/detect",
            json={
                "image_url": "data:image/jpeg;base64," + base64.b64encode(image.read_bytes()).decode(),
                "city": "Hangzhou",
                "resource_type": "attraction",
            },
        )

    assert response.status_code == 200
    data = response.json()["data"]
    assert data["model_mode"] == "trained_yolo"
    assert data["labels"][0]["name"] == "crowded_scene"
    assert data["labels"][0]["confidence"] >= 0.9
    assert data["risk_hints"]
