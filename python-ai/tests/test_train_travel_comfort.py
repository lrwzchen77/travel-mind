import json

from app.travel_comfort import FEATURE_NAMES
from scripts.train_travel_comfort import load_feedback


def test_loads_valid_server_prediction_snapshot(tmp_path):
    path = tmp_path / "feedback.json"
    snapshot = {name: index + 0.5 for index, name in enumerate(FEATURE_NAMES)}
    path.write_text(json.dumps([{
        "actual_label": "balanced",
        "prediction_json": json.dumps({"data": {"feature_snapshot": snapshot}}),
    }]), encoding="utf-8")

    rows = load_feedback(path)

    assert rows == [(snapshot, "balanced")]
