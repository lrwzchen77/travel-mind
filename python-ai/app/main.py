import os
import re
import tempfile
from pathlib import Path
from typing import Any

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv
from pydantic import BaseModel, Field

PROJECT_ROOT = Path(__file__).resolve().parents[2]
load_dotenv(PROJECT_ROOT / ".env", override=False)

app = FastAPI(title="Travel Mind Python AI")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Lazy-loaded YOLO handle (classify or detect weights)
_YOLO_MODEL = None
_YOLO_MODEL_PATH: str | None = None

# TravelRisk six-class risk copy (classify head names)
_CLASS_RISK_HINTS: dict[str, str] = {
    "crowded_scene": "画面偏拥挤，出行请预留排队时间并注意随身物品。",
    "low_light_scene": "低光/夜景场景，注意照明与返程交通安全。",
}


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
    image_path = str(payload.get("image_path") or "")
    city = str(payload.get("city") or "Unknown city")
    resource_type = str(payload.get("resource_type") or "travel_scene")
    source_text = f"{image_url} {filename} {image_path} {resource_type}".lower()

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

    # Prefer local path / upload temp file / then URL
    yolo_source = image_path or image_url
    yolo = _try_yolo_detection(yolo_source)
    if yolo is not None:
        labels = yolo["labels"]
        for tag in yolo.get("scene_tags") or []:
            scene_tags.append(tag)
        for hint in yolo.get("risk_hints") or []:
            risks.append(hint)
        top_name = labels[0]["name"] if labels else "unknown"
        top_conf = labels[0]["confidence"] if labels else 0.0
        return ok(
            {
                "model_mode": yolo.get("model_mode", "trained_yolo"),
                "labels": labels,
                "scene_tags": _unique(scene_tags),
                "summary": (
                    f"{city} 图片已通过自训 YOLO 分类识别为 {top_name}"
                    f"（置信度 {top_conf}），可用于补充旅行资源画像。"
                ),
                "risk_hints": _unique(risks),
                "source": "upload" if filename else ("local_path" if image_path else "image_url"),
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
        payload: dict[str, Any] = {key: value for key, value in form.items() if key != "file"}
        file = form.get("file")
        if file is not None:
            payload["filename"] = getattr(file, "filename", "") or "upload.jpg"
            try:
                raw = await file.read()  # type: ignore[misc]
            except Exception:
                raw = b""
            if raw:
                suffix = Path(str(payload["filename"])).suffix or ".jpg"
                tmp = tempfile.NamedTemporaryFile(delete=False, suffix=suffix)
                try:
                    tmp.write(raw)
                    tmp.flush()
                    payload["image_path"] = tmp.name
                finally:
                    tmp.close()
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


def _get_yolo_model(model_path: str):
    """Load YOLO once per process; reload if env path changes."""
    global _YOLO_MODEL, _YOLO_MODEL_PATH
    if _YOLO_MODEL is not None and _YOLO_MODEL_PATH == model_path:
        return _YOLO_MODEL
    from ultralytics import YOLO  # type: ignore

    _YOLO_MODEL = YOLO(model_path)
    _YOLO_MODEL_PATH = model_path
    return _YOLO_MODEL


def _try_yolo_detection(image_source: str) -> dict[str, Any] | None:
    """
    Run TRAVEL_MIND_YOLO_MODEL on image_source (http(s) URL or local path).

    Priority: classification probs (TravelRisk yolov8n-cls) → detection boxes.
    On any failure return None so the caller falls back to rule mode (no 500).
    """
    model_path = os.getenv("TRAVEL_MIND_YOLO_MODEL", "").strip()
    if not model_path or not image_source:
        return None
    path_obj = _resolve_project_path(model_path)
    if not path_obj.is_file():
        return None
    # Local path must exist; remote URL allowed as-is
    if not image_source.startswith(("http://", "https://")):
        if not Path(image_source).is_file():
            return None
    try:
        model = _get_yolo_model(str(path_obj))
        results = model.predict(source=image_source, verbose=False)
        labels: list[dict[str, Any]] = []
        for result in results:
            names = getattr(result, "names", {}) or {}
            # --- classify path (TravelRisk yolov8n-cls) ---
            probs = getattr(result, "probs", None)
            if probs is not None:
                top1 = int(probs.top1)
                conf = float(probs.top1conf)
                name = str(names.get(top1, top1))
                labels.append({"name": name, "confidence": round(conf, 4)})
                # optional top-k for richer labels
                try:
                    data = probs.data.detach().cpu().numpy().tolist()
                    ranked = sorted(
                        [
                            {
                                "name": str(names.get(i, i)),
                                "confidence": round(float(c), 4),
                            }
                            for i, c in enumerate(data)
                        ],
                        key=lambda x: -x["confidence"],
                    )
                    # keep top1 first, then other top classes with conf>=0.05
                    extras = [x for x in ranked[1:4] if x["confidence"] >= 0.05]
                    labels.extend(extras)
                except Exception:
                    pass
                continue
            # --- detect path (legacy boxes) ---
            boxes = getattr(result, "boxes", None)
            if boxes is None:
                continue
            for box in boxes:
                cls_id = int(box.cls[0])
                confidence = float(box.conf[0])
                labels.append(
                    {
                        "name": str(names.get(cls_id, cls_id)),
                        "confidence": round(confidence, 4),
                    }
                )
        if not labels:
            return None
        top = labels[0]["name"]
        scene_tags = [top]
        risk_hints: list[str] = []
        if top in _CLASS_RISK_HINTS:
            risk_hints.append(_CLASS_RISK_HINTS[top])
        # also scan other high-conf labels for risk classes
        for item in labels[1:]:
            n = item["name"]
            if n in _CLASS_RISK_HINTS and item["confidence"] >= 0.15:
                risk_hints.append(_CLASS_RISK_HINTS[n])
                scene_tags.append(n)
        return {
            "model_mode": "trained_yolo",
            "labels": labels,
            "scene_tags": scene_tags,
            "risk_hints": _unique(risk_hints),
        }
    except Exception:
        return None


def _resolve_project_path(value: str) -> Path:
    path = Path(value).expanduser()
    if path.is_absolute() or path.exists():
        return path
    return PROJECT_ROOT / path


def _join_tags(tags: list[str]) -> str:
    return "、".join(_unique(tags[:4]))


def _unique(values: list[str]) -> list[str]:
    result = []
    for value in values:
        if value and value not in result:
            result.append(value)
    return result
