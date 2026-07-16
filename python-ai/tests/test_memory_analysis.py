from pathlib import Path

import httpx
import pytest
from PIL import Image

import app.main as main
import app.memory as memory


def _photo(directory: Path, name: str, exif: Image.Exif | None = None) -> Path:
    directory.mkdir(parents=True, exist_ok=True)
    path = directory / name
    image = Image.new("RGB", (16, 16), "blue")
    image.save(path, **({"exif": exif} if exif else {}))
    return path


def _payload(photo_name: str) -> dict:
    return {
        "memoryId": 301,
        "tripId": 901,
        "title": "杭州旅行",
        "destinationCity": "杭州",
        "items": [
            {
                "itemId": 11,
                "itemType": "place",
                "placeName": "西湖",
                "city": "杭州",
                "content": "湖边慢游",
                "takenAt": "2026-08-12T10:00:00",
                "dayIndex": 1,
            },
            {"itemId": 21, "itemType": "photo", "sourceUrl": f"/uploads/{photo_name}"},
        ],
    }


@pytest.mark.anyio
async def test_memory_analysis_reads_exif_matches_time_and_returns_evidence(tmp_path, monkeypatch):
    name = "123e4567-e89b-12d3-a456-426614174001.jpg"
    exif = Image.Exif()
    exif[36867] = "2026:08:12 10:30:00"
    exif[274] = 6
    exif[34853] = {1: "N", 2: (30.0, 15.0, 0.0), 3: "E", 4: (120.0, 9.0, 0.0)}
    _photo(tmp_path, name, exif)
    monkeypatch.setattr(memory, "UPLOAD_DIR", tmp_path.resolve())
    monkeypatch.setattr(main, "_try_yolo_detection", lambda _: None)

    transport = httpx.ASGITransport(app=main.app)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver") as client:
        response = await client.post("/api/memory/analyze", json=_payload(name))

    assert response.status_code == 200
    data = response.json()["data"]
    photo = data["items"][0]
    assert photo["takenAt"] == "2026-08-12 10:30:00"
    assert photo["orientation"] == 6
    assert photo["latitude"] == 30.25
    assert photo["longitude"] == 120.15
    assert photo["matchedItemId"] == 11
    assert photo["placeName"] == "西湖"
    assert photo["dayIndex"] == 1
    assert photo["modelMode"] == "unavailable"
    assert any("2 小时" in reason for reason in photo["evidenceReasons"])
    assert data["generation"]["evidenceItemIds"] == [11, 21]


@pytest.mark.anyio
async def test_memory_analysis_uses_supplied_gps_and_existing_yolo(tmp_path, monkeypatch):
    name = "123e4567-e89b-12d3-a456-426614174002.jpg"
    _photo(tmp_path, name)
    monkeypatch.setattr(memory, "UPLOAD_DIR", tmp_path.resolve())
    monkeypatch.setattr(main, "_try_yolo_detection", lambda _: {
        "model_mode": "trained_yolo",
        "scene_tags": ["scenic_spot", "crowded_scene"],
        "risk_hints": ["注意拥挤"],
    })
    payload = _payload(name)
    payload["items"][0].update(latitude=30.25, longitude=120.15)
    payload["items"][1].update(latitude=30.2505, longitude=120.1505)

    transport = httpx.ASGITransport(app=main.app)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver") as client:
        response = await client.post("/api/memory/analyze", json=payload)

    photo = response.json()["data"]["items"][0]
    assert photo["matchedItemId"] == 11
    assert photo["scene"] == "scenic_spot"
    assert photo["riskHints"] == ["注意拥挤"]
    assert photo["modelMode"] == "trained_yolo"
    assert any("坐标" in reason for reason in photo["evidenceReasons"])


@pytest.mark.anyio
async def test_memory_analysis_degrades_for_no_exif_and_bad_image(tmp_path, monkeypatch):
    good = "123e4567-e89b-12d3-a456-426614174003.jpg"
    bad = "123e4567-e89b-12d3-a456-426614174004.jpg"
    _photo(tmp_path, good)
    (tmp_path / bad).write_bytes(b"not-an-image")
    monkeypatch.setattr(memory, "UPLOAD_DIR", tmp_path.resolve())
    calls = []
    monkeypatch.setattr(main, "_try_yolo_detection", lambda path: calls.append(path) or None)
    payload = _payload(good)
    payload["items"].append({"itemId": 22, "itemType": "photo", "sourceUrl": f"/uploads/{bad}"})

    transport = httpx.ASGITransport(app=main.app)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver") as client:
        response = await client.post("/api/memory/analyze", json=payload)

    good_result, bad_result = response.json()["data"]["items"]
    assert good_result["takenAt"] is None
    assert good_result["matchedItemId"] is None
    assert good_result["modelMode"] == "unavailable"
    assert bad_result["imageStatus"] == "unreadable"
    assert bad_result["matchedItemId"] is None
    assert len(calls) == 1


@pytest.mark.anyio
async def test_memory_analysis_rejects_uncontrolled_source(tmp_path, monkeypatch):
    monkeypatch.setattr(memory, "UPLOAD_DIR", tmp_path.resolve())
    payload = _payload("123e4567-e89b-12d3-a456-426614174005.jpg")
    payload["items"][1]["sourceUrl"] = "http://127.0.0.1/private.jpg"

    transport = httpx.ASGITransport(app=main.app, raise_app_exceptions=False)
    async with httpx.AsyncClient(transport=transport, base_url="http://testserver") as client:
        response = await client.post("/api/memory/analyze", json=payload)

    assert response.status_code == 400
