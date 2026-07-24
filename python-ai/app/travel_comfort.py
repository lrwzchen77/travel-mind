import os
from pathlib import Path
from typing import Any, Mapping

import joblib


PROJECT_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_MODEL_PATH = PROJECT_ROOT / "python-ai" / "models" / "travel-comfort-v1.joblib"
FEATURE_NAMES = (
    "day_count",
    "total_attractions",
    "average_attractions",
    "max_attractions",
    "dense_days",
    "transfer_days",
    "city_transfers",
    "adverse_weather_days",
    "relaxed_preference",
    "budget_per_day",
    "budget_pressure",
    "transport_stress",
)

_ADVERSE_WEATHER = ("rain", "storm", "snow", "heat", "雨", "雪", "高温", "暴")
_RELAXED_PREFERENCES = ("轻松", "亲子", "慢游", "relaxed", "family", "slow")
_MODEL_BUNDLE: dict[str, Any] | None = None
_MODEL_PATH: Path | None = None


def extract_features(payload: Mapping[str, Any]) -> dict[str, float]:
    days = payload.get("days") or []
    day_count = len(days)
    attraction_counts = [len((day or {}).get("attractions") or []) for day in days]
    total_attractions = sum(attraction_counts)
    transfer_days = sum(bool((day or {}).get("transfer")) for day in days)
    adverse_weather_days = sum(
        any(word in str((day or {}).get("weather") or "").lower() for word in _ADVERSE_WEATHER)
        for day in days
    )
    preferences = " ".join(str(item).lower() for item in (payload.get("preferences") or []))
    budget = _number(payload.get("budget"))
    divisor = max(day_count, 1)
    budget_per_day = min(budget / divisor, 5000.0) if budget is not None else 1200.0
    transportation = str(payload.get("transportation") or "").lower()

    return {
        "day_count": float(day_count),
        "total_attractions": float(total_attractions),
        "average_attractions": round(total_attractions / divisor, 4),
        "max_attractions": float(max(attraction_counts, default=0)),
        "dense_days": float(sum(count >= 4 for count in attraction_counts)),
        "transfer_days": float(transfer_days),
        "city_transfers": float(max(int(_number(payload.get("city_transfers")) or 0), 0)),
        "adverse_weather_days": float(adverse_weather_days),
        "relaxed_preference": float(any(word in preferences for word in _RELAXED_PREFERENCES)),
        "budget_per_day": round(budget_per_day, 2),
        "budget_pressure": float(budget is not None and budget_per_day < 800),
        "transport_stress": _transport_stress(transportation),
    }


def predict_comfort(payload: Mapping[str, Any]) -> dict[str, Any] | None:
    bundle = _load_bundle()
    if bundle is None:
        return None
    features = extract_features(payload)
    try:
        vector = [[features[name] for name in FEATURE_NAMES]]
        model = bundle["model"]
        probabilities = model.predict_proba(vector)[0]
        classes = [str(item) for item in model.classes_]
        probability_map = {name: round(float(probability), 4) for name, probability in zip(classes, probabilities)}
        predicted = max(probability_map, key=probability_map.get)
        class_scores = {"relaxed": 90, "balanced": 72, "intense": 48}
        comfort_score = round(sum(probability_map.get(label, 0) * score for label, score in class_scores.items()))
        return {
            "model_mode": "trained_travel_comfort",
            "model_version": str(bundle.get("version") or "travel-comfort-v1"),
            "comfort_class": predicted,
            "confidence": probability_map[predicted],
            "probabilities": probability_map,
            "comfort_score": max(35, min(100, comfort_score)),
            "risk_level": {"relaxed": "low", "balanced": "medium", "intense": "high"}[predicted],
            "feature_snapshot": features,
            "training_source": str(bundle.get("training_source") or "bootstrap_scenarios"),
        }
    except (KeyError, TypeError, ValueError):
        return None


def reset_model_cache() -> None:
    global _MODEL_BUNDLE, _MODEL_PATH
    _MODEL_BUNDLE = None
    _MODEL_PATH = None


def _load_bundle() -> dict[str, Any] | None:
    global _MODEL_BUNDLE, _MODEL_PATH
    configured = os.getenv("TRAVEL_MIND_COMFORT_MODEL", "").strip()
    path = Path(configured) if configured else DEFAULT_MODEL_PATH
    if not path.is_absolute():
        path = PROJECT_ROOT / path
    path = path.resolve()
    if not path.is_file():
        return None
    if _MODEL_BUNDLE is not None and _MODEL_PATH == path:
        return _MODEL_BUNDLE
    try:
        bundle = joblib.load(path)
        if tuple(bundle.get("feature_names") or ()) != FEATURE_NAMES or "model" not in bundle:
            return None
        _MODEL_BUNDLE = bundle
        _MODEL_PATH = path
        return bundle
    except (OSError, TypeError, ValueError):
        return None


def _number(value: Any) -> float | None:
    try:
        return float(value) if value is not None else None
    except (TypeError, ValueError):
        return None


def _transport_stress(value: str) -> float:
    if any(word in value for word in ("flight", "plane", "飞机", "航空")):
        return 1.0
    if any(word in value for word in ("bus", "coach", "公交", "大巴")):
        return 0.7
    if any(word in value for word in ("subway", "metro", "地铁", "公共交通")):
        return 0.45
    if any(word in value for word in ("train", "rail", "火车", "高铁")):
        return 0.35
    return 0.2
