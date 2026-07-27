import math
import re
from datetime import datetime
from pathlib import Path
from typing import Any, Callable
from uuid import UUID

from PIL import Image, UnidentifiedImageError
from pydantic import BaseModel, Field


PROJECT_ROOT = Path(__file__).resolve().parents[2]
UPLOAD_DIR = (PROJECT_ROOT / "uploads").resolve()
UPLOAD_PATH = re.compile(
    r"^/private-uploads/(?P<user>[0-9]+)/(?P<name>[0-9a-fA-F-]{36}\.(?:jpg|png|webp))$"
)


class MemoryItemInput(BaseModel):
    itemId: int
    itemType: str
    sourceUrl: str | None = None
    takenAt: datetime | None = None
    latitude: float | None = None
    longitude: float | None = None
    city: str | None = None
    placeName: str | None = None
    content: str | None = None
    dayIndex: int | None = None


class MemoryAnalysisRequest(BaseModel):
    memoryId: int
    tripId: int
    title: str
    destinationCity: str
    items: list[MemoryItemInput] = Field(default_factory=list, max_length=500)


def analyze_memory(
    payload: MemoryAnalysisRequest,
    yolo_detector: Callable[[str], dict[str, Any] | None],
) -> dict[str, Any]:
    candidates = [item for item in payload.items if item.itemType != "photo"]
    results = [_analyze_photo(item, candidates, payload.destinationCity, yolo_detector)
               for item in payload.items if item.itemType == "photo"]
    return {
        "items": results,
        "generation": _timeline(payload, results),
    }


def _analyze_photo(
    photo: MemoryItemInput,
    candidates: list[MemoryItemInput],
    destination_city: str,
    yolo_detector: Callable[[str], dict[str, Any] | None],
) -> dict[str, Any]:
    path = _controlled_photo_path(photo.sourceUrl)
    exif, image_status = _extract_exif(path)
    taken_at = photo.takenAt or exif.get("takenAt")
    latitude = photo.latitude if photo.latitude is not None else exif.get("latitude")
    longitude = photo.longitude if photo.longitude is not None else exif.get("longitude")

    yolo = yolo_detector(str(path)) if image_status == "ready" else None
    model_mode = str(yolo.get("model_mode")) if yolo else "unavailable"
    scene_tags = _unique([str(tag) for tag in (yolo or {}).get("scene_tags", [])])
    risks = _unique([str(hint) for hint in (yolo or {}).get("risk_hints", [])])
    scene = scene_tags[0] if scene_tags else None

    day_index, day_reason = _infer_day(taken_at, candidates, photo.dayIndex)
    match = _best_match(photo, taken_at, latitude, longitude, scene_tags, candidates)
    reasons = list(match["reasons"])
    if day_reason and day_reason not in reasons:
        reasons.append(day_reason)
    matched = match["item"]
    place_name = matched.placeName if matched else photo.placeName
    caption_bits = [place_name or destination_city or "旅行"]
    if scene:
        caption_bits.append(scene)
    caption_bits.append("照片")

    return {
        "itemId": photo.itemId,
        "caption": "，".join(caption_bits),
        "tags": _unique(scene_tags + (["有风险提示"] if risks else [])),
        "placeName": place_name,
        "confidence": match["confidence"] if matched else None,
        "takenAt": taken_at.strftime("%Y-%m-%d %H:%M:%S") if taken_at else None,
        "latitude": latitude,
        "longitude": longitude,
        "dayIndex": matched.dayIndex if matched and matched.dayIndex else day_index,
        "matchedItemId": matched.itemId if matched else None,
        "evidenceReasons": reasons,
        "scene": scene,
        "riskHints": risks,
        "modelMode": model_mode,
        "orientation": exif.get("orientation"),
        "imageStatus": image_status,
    }


def _controlled_photo_path(source_url: str | None) -> Path:
    match = UPLOAD_PATH.fullmatch(source_url or "")
    if not match:
        raise ValueError("photo source must be a controlled upload path")
    name = match.group("name")
    UUID(name.rsplit(".", 1)[0])
    user = match.group("user")
    directory = (UPLOAD_DIR / "private" / user).resolve()
    path = (directory / name).resolve()
    if path.parent != directory or not path.is_file():
        raise ValueError("controlled photo does not exist")
    return path


def _extract_exif(path: Path) -> tuple[dict[str, Any], str]:
    try:
        with Image.open(path) as image:
            image.verify()
        with Image.open(path) as image:
            exif = image.getexif()
            result: dict[str, Any] = {}
            raw_time = exif.get(36867) or exif.get(36868) or exif.get(306)
            if raw_time:
                try:
                    result["takenAt"] = datetime.strptime(str(raw_time), "%Y:%m:%d %H:%M:%S")
                except ValueError:
                    pass
            orientation = exif.get(274)
            if orientation in range(1, 9):
                result["orientation"] = int(orientation)
            try:
                gps = exif.get_ifd(34853)
                latitude = _gps_coordinate(gps.get(2), gps.get(1))
                longitude = _gps_coordinate(gps.get(4), gps.get(3))
                if latitude is not None and longitude is not None:
                    result.update(latitude=latitude, longitude=longitude)
            except (AttributeError, KeyError, TypeError, ValueError, ZeroDivisionError):
                pass
            return result, "ready"
    except (OSError, UnidentifiedImageError, ValueError):
        return {}, "unreadable"


def _gps_coordinate(value: Any, reference: Any) -> float | None:
    if not value or not reference or len(value) != 3:
        return None
    degrees, minutes, seconds = (float(part) for part in value)
    coordinate = degrees + minutes / 60 + seconds / 3600
    if str(reference).upper() in {"S", "W"}:
        coordinate = -coordinate
    return round(coordinate, 7)


def _infer_day(
    taken_at: datetime | None,
    candidates: list[MemoryItemInput],
    supplied_day: int | None,
) -> tuple[int | None, str | None]:
    if supplied_day:
        return supplied_day, "用户提供了照片所属旅行日"
    if not taken_at:
        return None, None
    days = {item.dayIndex for item in candidates if item.dayIndex and item.takenAt
            and item.takenAt.date() == taken_at.date()}
    if len(days) == 1:
        day = days.pop()
        return day, f"拍摄日期与第 {day} 天行程一致"
    return None, None


def _best_match(
    photo: MemoryItemInput,
    taken_at: datetime | None,
    latitude: float | None,
    longitude: float | None,
    scene_tags: list[str],
    candidates: list[MemoryItemInput],
) -> dict[str, Any]:
    best: dict[str, Any] = {"item": None, "confidence": None, "reasons": []}
    # ponytail: 单次记忆册最多 500 项，O(n*m) 足够；实测成为瓶颈后再加空间索引。
    for item in candidates:
        if item.itemType != "place":
            continue
        score = 0.0
        strong = False
        reasons: list[str] = []
        if latitude is not None and longitude is not None and item.latitude is not None and item.longitude is not None:
            distance = _distance_m(latitude, longitude, item.latitude, item.longitude)
            if distance <= 250:
                score += 0.6
                strong = True
                reasons.append(f"照片坐标距行程地点约 {round(distance)} 米")
            elif distance <= 1000:
                score += 0.5
                strong = True
                reasons.append(f"照片坐标距行程地点约 {round(distance)} 米")
            elif distance <= 5000:
                score += 0.3
                reasons.append("照片坐标与行程地点在 5 公里内")
        if taken_at and item.takenAt:
            hours = abs((taken_at - item.takenAt).total_seconds()) / 3600
            if hours <= 2:
                score += 0.55
                strong = True
                reasons.append("拍摄时间与行程地点时间相差不超过 2 小时")
            elif hours <= 8:
                score += 0.35
                reasons.append("拍摄时间与行程地点在同一时段")
            elif taken_at.date() == item.takenAt.date():
                score += 0.2
                reasons.append("拍摄日期与行程地点日期一致")
        if photo.dayIndex and item.dayIndex == photo.dayIndex:
            score += 0.15
        if photo.city and item.city and photo.city == item.city:
            score += 0.05
        context = f"{item.placeName or ''} {item.content or ''}".lower()
        if scene_tags and any(tag.lower() in context for tag in scene_tags):
            score += 0.05
            reasons.append("视觉场景与地点文字一致")
        score = min(score, 1.0)
        if strong and score >= 0.5 and (best["confidence"] is None or score > best["confidence"]):
            best = {"item": item, "confidence": round(score, 4), "reasons": reasons}
    return best


def _distance_m(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    radius = 6_371_000
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lon2 - lon1)
    value = math.sin(delta_phi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2) ** 2
    value = min(max(value, 0.0), 1.0)
    return radius * 2 * math.atan2(math.sqrt(value), math.sqrt(1 - value))


def _timeline(payload: MemoryAnalysisRequest, photo_results: list[dict[str, Any]]) -> dict[str, Any]:
    photos = {item["itemId"]: item for item in photo_results}
    grouped: dict[int, list[str]] = {}
    evidence: list[int] = []
    for item in payload.items:
        day = item.dayIndex or (photos.get(item.itemId) or {}).get("dayIndex")
        if not day:
            continue
        if item.itemType == "photo":
            text = photos[item.itemId]["caption"]
        elif item.itemType == "expense":
            text = f"{item.placeName or '旅行支出'}：{item.content or '已记录'}"
        elif item.itemType == "place":
            text = f"{item.placeName or '行程地点'}：{item.content or '已到访'}"
        else:
            continue
        grouped.setdefault(day, []).append(f"- {text}")
        evidence.append(item.itemId)
    lines = [f"# {payload.title or payload.destinationCity + '旅行'}时间线"]
    for day, entries in sorted(grouped.items()):
        lines.extend(["", f"## 第 {day} 天", *entries])
    if not grouped:
        lines.extend(["", "当前证据不足以生成按日时间线，请补充照片拍摄时间或所属旅行日。"])
    return {
        "type": "timeline",
        "content": "\n".join(lines),
        "evidenceItemIds": list(dict.fromkeys(evidence)),
    }


def _unique(values: list[str]) -> list[str]:
    return list(dict.fromkeys(value for value in values if value))
