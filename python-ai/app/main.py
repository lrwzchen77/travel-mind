import os
import re
from typing import Any

from fastapi import FastAPI, Request
from pydantic import BaseModel, Field

app = FastAPI(title="Travel Mind Python AI")


def ok(data: dict[str, Any]) -> dict[str, Any]:
    return {"code": 0, "message": "success", "data": data}


@app.get("/health")
def health():
    return {
        "code": 0,
        "message": "success",
        "data": {
            "service": "travel-mind-python-ai",
            "status": "healthy",
            "mode": "phase-1",
        },
    }


class TripDayInput(BaseModel):
    date: str | None = None
    city: str | None = None
    attractions: list[str] = Field(default_factory=list)
    weather: str | None = None
    transfer: bool = False


class TripEvaluateRequest(BaseModel):
    days: list[TripDayInput] = Field(default_factory=list)
    transportation: str | None = None
    city_transfers: int = 0
    preferences: list[str] = Field(default_factory=list)
    budget: float | None = None


class ContentAnalyzeRequest(BaseModel):
    text: str
    city: str | None = None
    attraction_name: str | None = None
    language: str = "zh"


@app.post("/api/vision/detect")
async def vision_detect(request: Request):
    payload = await _read_vision_payload(request)
    image_url = str(payload.get("image_url") or "")
    filename = str(payload.get("filename") or "")
    city = str(payload.get("city") or "Unknown city")
    resource_type = str(payload.get("resource_type") or "travel_scene")
    source_text = f"{image_url} {filename} {resource_type}".lower()

    scene_tags = ["travel_scene"]
    risks: list[str] = []
    if any(word in source_text for word in ("night", "夜", "bar")):
        scene_tags.append("night_view")
        risks.append("夜间出行注意返程交通和照明。")
    if any(word in source_text for word in ("food", "restaurant", "meal", "美食", "餐")):
        scene_tags.append("food")
    if any(word in source_text for word in ("lake", "river", "water", "西湖", "湖")):
        scene_tags.append("waterfront")
    if any(word in source_text for word in ("hotel", "room", "住宿")):
        scene_tags.append("hotel")
    if resource_type and resource_type not in scene_tags:
        scene_tags.append(resource_type)

    yolo = _try_yolo_detection(image_url)
    if yolo is not None:
        return ok(
            {
                "model_mode": "yolo",
                "labels": yolo,
                "scene_tags": _unique(scene_tags),
                "summary": f"{city} 图片已通过 YOLO 模型识别，可用于补充旅行资源画像。",
                "risk_hints": risks,
                "source": "upload" if filename else "image_url",
            }
        )

    labels = [
        {"name": "travel_scene", "confidence": 0.91},
        {"name": scene_tags[-1], "confidence": 0.82},
    ]
    return ok(
        {
            "model_mode": "rule",
            "labels": labels,
            "scene_tags": _unique(scene_tags),
            "summary": f"{city} {resource_type} 图片呈现{_join_tags(scene_tags)}特征，可用于补充旅行资源画像。",
            "risk_hints": risks,
            "source": "upload" if filename else "image_url",
        }
    )


@app.post("/api/trip/evaluate")
def trip_evaluate(payload: TripEvaluateRequest):
    score = 92
    daily_risks: list[dict[str, Any]] = []
    relaxed = any(pref in {"轻松", "亲子", "慢游", "relaxed"} for pref in payload.preferences)

    for index, day in enumerate(payload.days, start=1):
        risk_items: list[str] = []
        attraction_count = len(day.attractions)
        if attraction_count >= 4:
            score -= 18
            risk_items.append("当天景点超过 3 个，节奏偏紧。")
        elif attraction_count == 3:
            score -= 8
            risk_items.append("当天景点较多，建议预留休息时间。")
        if relaxed and attraction_count >= 3:
            score -= 8
            risk_items.append("用户偏好轻松，当前安排需要降密度。")
        if day.transfer:
            score -= 8
            risk_items.append("包含城市或酒店转移，注意交通缓冲。")
        if day.weather and any(word in day.weather.lower() for word in ("rain", "storm", "雨", "雪", "高温")):
            score -= 7
            risk_items.append("天气可能影响步行和户外体验。")
        daily_risks.append(
            {
                "day_index": index,
                "date": day.date,
                "city": day.city,
                "attractions_count": attraction_count,
                "risk_items": risk_items,
            }
        )

    if payload.city_transfers > 0:
        score -= min(payload.city_transfers * 6, 18)
    if payload.budget is not None and payload.budget < max(len(payload.days), 1) * 800:
        score -= 6

    score = max(35, min(100, score))
    risk_level = "low" if score >= 80 else "medium" if score >= 60 else "high"
    suggestions = _trip_suggestions(risk_level, daily_risks)
    return ok(
        {
            "comfort_score": score,
            "risk_level": risk_level,
            "daily_risks": daily_risks,
            "suggestions": suggestions,
        }
    )


@app.post("/api/content/analyze")
def content_analyze(payload: ContentAnalyzeRequest):
    text = payload.text.strip()
    positive_words = ["好", "美", "推荐", "舒服", "适合", "方便", "惊喜", "干净", "安静"]
    negative_words = ["排队", "拥挤", "人很多", "贵", "坑", "差", "堵", "久", "风险"]
    positive = [word for word in positive_words if word in text]
    negative = [word for word in negative_words if word in text]
    if positive and negative:
        sentiment = "mixed"
    elif positive:
        sentiment = "positive"
    elif negative:
        sentiment = "negative"
    else:
        sentiment = "neutral"

    keywords = _extract_keywords(text, payload.city, payload.attraction_name)
    traveler_types = ["general"]
    if "亲子" in text or "孩子" in text or "family" in text.lower():
        traveler_types.append("family")
    if "拍照" in text or "夜景" in text:
        traveler_types.append("photo")
    if "散步" in text or "轻松" in text:
        traveler_types.append("relaxed")

    return ok(
        {
            "sentiment": sentiment,
            "keywords": keywords,
            "positive_highlights": [f"提到{word}体验" for word in positive],
            "negative_warnings": [f"注意{word}问题" for word in negative],
            "suitable_traveler_types": _unique(traveler_types),
        }
    )


async def _read_vision_payload(request: Request) -> dict[str, Any]:
    content_type = request.headers.get("content-type", "")
    if "multipart/form-data" in content_type:
        form = await request.form()
        payload = {key: value for key, value in form.items() if key != "file"}
        file = form.get("file")
        if file is not None:
            payload["filename"] = getattr(file, "filename", "")
        return payload
    if "application/json" in content_type:
        return await request.json()
    return {}


def _extract_keywords(text: str, city: str | None, attraction_name: str | None) -> list[str]:
    candidates = [city, attraction_name]
    candidates.extend(re.findall(r"[A-Za-z][A-Za-z0-9_-]{2,}", text))
    for word in ["西湖", "灵隐寺", "夜景", "美食", "亲子", "散步", "排队", "酒店", "餐厅", "交通"]:
        if word in text:
            candidates.append(word)
    return _unique([item for item in candidates if item])


def _trip_suggestions(risk_level: str, daily_risks: list[dict[str, Any]]) -> list[str]:
    suggestions = []
    if risk_level in {"medium", "high"}:
        suggestions.append("减少单日景点数量，保留午后或傍晚休息窗口。")
    if any(item["risk_items"] for item in daily_risks):
        suggestions.append("将天气敏感的户外项目安排可替换室内备选。")
    if not suggestions:
        suggestions.append("当前节奏较舒适，建议保持交通和用餐缓冲。")
    return suggestions


def _try_yolo_detection(image_url: str) -> list[dict[str, Any]] | None:
    model_path = os.getenv("TRAVEL_MIND_YOLO_MODEL", "").strip()
    if not model_path or not image_url:
        return None
    try:
        from ultralytics import YOLO  # type: ignore

        model = YOLO(model_path)
        results = model(image_url, verbose=False)
        labels: list[dict[str, Any]] = []
        for result in results:
            names = getattr(result, "names", {})
            boxes = getattr(result, "boxes", None)
            if boxes is None:
                continue
            for box in boxes:
                cls_id = int(box.cls[0])
                confidence = float(box.conf[0])
                labels.append({"name": str(names.get(cls_id, cls_id)), "confidence": round(confidence, 4)})
        return labels or None
    except Exception:
        return None


def _join_tags(tags: list[str]) -> str:
    return "、".join(_unique(tags[:4]))


def _unique(values: list[str]) -> list[str]:
    result = []
    for value in values:
        if value and value not in result:
            result.append(value)
    return result
