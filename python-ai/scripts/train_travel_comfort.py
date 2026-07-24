import argparse
import json
import sys
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

import joblib
import matplotlib.pyplot as plt
import numpy as np
import sklearn
from sklearn.ensemble import GradientBoostingClassifier
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix, f1_score, log_loss
from sklearn.model_selection import train_test_split


PYTHON_ROOT = Path(__file__).resolve().parents[1]
REPO_ROOT = PYTHON_ROOT.parent
sys.path.insert(0, str(PYTHON_ROOT))

from app.travel_comfort import FEATURE_NAMES, extract_features  # noqa: E402


def scenario(rng: np.random.Generator) -> tuple[dict, str]:
    day_count = int(rng.choice(np.arange(1, 8), p=[0.08, 0.18, 0.25, 0.22, 0.14, 0.08, 0.05]))
    density = float(rng.uniform(1.2, 4.8))
    counts = np.clip(np.rint(rng.normal(density, 0.75, day_count)), 1, 6).astype(int)
    transfer_probability = float(rng.uniform(0.0, 0.55))
    weather_probability = float(rng.uniform(0.0, 0.5))
    transfer_days = rng.random(day_count) < transfer_probability
    bad_weather = rng.random(day_count) < weather_probability
    relaxed = bool(rng.random() < 0.38)
    budget_per_day = float(np.clip(rng.normal(1000, 500), 250, 2800))
    transportation = str(rng.choice(["train", "公共交通", "subway", "flight", "self_drive"], p=[0.25, 0.28, 0.2, 0.12, 0.15]))
    city_transfers = int(rng.integers(0, min(3, day_count - 1) + 1))
    if city_transfers:
        transfer_days[rng.choice(day_count, size=city_transfers, replace=False)] = True
    payload = {
        "days": [
            {
                "date": f"2026-08-{index + 1:02d}",
                "city": "示例城市",
                "attractions": [f"地点-{item}" for item in range(int(count))],
                "weather": "高温有雨" if bool(bad_weather[index]) else "晴",
                "transfer": bool(transfer_days[index]),
            }
            for index, count in enumerate(counts)
        ],
        "transportation": transportation,
        "city_transfers": city_transfers,
        "preferences": ["轻松", "慢游"] if relaxed else ["经典", "效率"],
        "budget": round(budget_per_day * day_count, 2),
    }
    features = extract_features(payload)
    fatigue = (
        max(features["average_attractions"] - 1.8, 0) * 6
        + max(features["max_attractions"] - 3, 0) * 3
        + features["dense_days"] / day_count * 12
        + features["transfer_days"] / day_count * 18
        + features["city_transfers"] / day_count * 12
        + features["adverse_weather_days"] / day_count * 14
        + features["relaxed_preference"] * max(features["average_attractions"] - 1.8, 0) * 5
        + features["budget_pressure"] * 8
        + features["transport_stress"] * 8
        + max(features["day_count"] - 4, 0) * 1.5
        + float(rng.normal(0, 4.5))
    )
    latent_score = 96 - fatigue
    label = "relaxed" if latent_score >= 74 else "balanced" if latent_score >= 55 else "intense"
    return features, label


def load_feedback(path: Path | None) -> list[tuple[dict, str]]:
    if path is None:
        return []
    payload = json.loads(path.read_text(encoding="utf-8"))
    items = payload if isinstance(payload, list) else payload.get("feedback", [])
    rows = []
    for item in items:
        label = str(item.get("actual_label") or "")
        prediction = item.get("prediction_json") or {}
        if isinstance(prediction, str):
            prediction = json.loads(prediction)
        data = prediction.get("data", prediction)
        features = data.get("feature_snapshot") or {}
        if label in {"relaxed", "balanced", "intense"} and all(name in features for name in FEATURE_NAMES):
            rows.append(({name: float(features[name]) for name in FEATURE_NAMES}, label))
    return rows


def train(samples: int, seed: int, estimators: int, feedback_rows: list[tuple[dict, str]] | None = None) -> tuple[dict, dict, list, list]:
    rng = np.random.default_rng(seed)
    rows = [scenario(rng) for _ in range(samples)]
    x = np.array([[features[name] for name in FEATURE_NAMES] for features, _ in rows], dtype=float)
    y = np.array([label for _, label in rows])
    x_train, x_test, y_train, y_test = train_test_split(x, y, test_size=0.25, random_state=seed, stratify=y)
    feedback_rows = feedback_rows or []
    if feedback_rows:
        x_train = np.vstack([x_train, [[features[name] for name in FEATURE_NAMES] for features, _ in feedback_rows]])
        y_train = np.concatenate([y_train, [label for _, label in feedback_rows]])
    model = GradientBoostingClassifier(
        n_estimators=estimators,
        learning_rate=0.05,
        max_depth=3,
        min_samples_leaf=12,
        subsample=0.9,
        random_state=seed,
    )
    model.fit(x_train, y_train)
    prediction = model.predict(x_test)
    matrix = confusion_matrix(y_test, prediction, labels=list(model.classes_))
    report = classification_report(y_test, prediction, output_dict=True, zero_division=0)
    curves = []
    for train_probabilities, test_probabilities in zip(model.staged_predict_proba(x_train), model.staged_predict_proba(x_test)):
        curves.append({
            "train_loss": round(float(log_loss(y_train, train_probabilities, labels=model.classes_)), 6),
            "test_loss": round(float(log_loss(y_test, test_probabilities, labels=model.classes_)), 6),
            "test_f1": round(float(f1_score(y_test, model.classes_[np.argmax(test_probabilities, axis=1)], average="macro")), 6),
        })
    metrics = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "model": "GradientBoostingClassifier",
        "version": "travel-comfort-v1",
        "training_source": "bootstrap_scenarios_v1" + ("+user_feedback" if feedback_rows else ""),
        "dataset_disclosure": "Reproducible simulated travel scenarios with declared label noise; optional real feedback is added only to the training split.",
        "samples": samples,
        "feedback_samples": len(feedback_rows),
        "train_samples": len(x_train),
        "test_samples": len(x_test),
        "seed": seed,
        "features": list(FEATURE_NAMES),
        "classes": list(model.classes_),
        "class_distribution": dict(sorted(Counter(y.tolist()).items())),
        "accuracy": round(float(accuracy_score(y_test, prediction)), 4),
        "macro_f1": round(float(f1_score(y_test, prediction, average="macro")), 4),
        "confusion_matrix": matrix.tolist(),
        "learning_curve": curves,
        "classification_report": report,
        "feature_importance": {
            name: round(float(value), 6)
            for name, value in sorted(zip(FEATURE_NAMES, model.feature_importances_), key=lambda item: -item[1])
        },
        "hyperparameters": model.get_params(),
        "sklearn_version": sklearn.__version__,
        "limitations": [
            "Bootstrap labels come from a documented simulator and do not represent observed traveler feedback.",
            "Replace or fine-tune with user feedback before claiming real-world personalization.",
            "The current feature contract has no route distance or transfer-duration input.",
        ],
    }
    bundle = {
        "model": model,
        "feature_names": FEATURE_NAMES,
        "version": metrics["version"],
        "trained_at": metrics["generated_at"],
        "training_source": metrics["training_source"],
        "sklearn_version": sklearn.__version__,
    }
    return bundle, metrics, curves, matrix.tolist()


def plots(metrics: dict, curves: list[dict], matrix: list[list[int]], output_dir: Path) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)
    epochs = np.arange(1, len(curves) + 1)
    fig, axes = plt.subplots(1, 2, figsize=(12, 4.8))
    axes[0].plot(epochs, [item["train_loss"] for item in curves], label="train")
    axes[0].plot(epochs, [item["test_loss"] for item in curves], label="test")
    axes[0].set(title="TravelComfort log loss", xlabel="boosting stage", ylabel="log loss")
    axes[0].legend()
    axes[1].plot(epochs, [item["test_f1"] for item in curves], color="#d97745")
    axes[1].set(title="TravelComfort test macro-F1", xlabel="boosting stage", ylabel="macro-F1", ylim=(0.6, 1.0))
    fig.tight_layout()
    fig.savefig(output_dir / "travel-comfort-training.png", dpi=160)
    plt.close(fig)

    labels = metrics["classes"]
    fig, ax = plt.subplots(figsize=(6.6, 5.8))
    image = ax.imshow(matrix, cmap="Oranges")
    ax.set(xticks=range(len(labels)), yticks=range(len(labels)), xticklabels=labels, yticklabels=labels,
           xlabel="Predicted", ylabel="True", title="TravelComfort bootstrap test")
    for row in range(len(labels)):
        for column in range(len(labels)):
            ax.text(column, row, matrix[row][column], ha="center", va="center")
    fig.colorbar(image, ax=ax)
    fig.tight_layout()
    fig.savefig(output_dir / "travel-comfort-confusion-matrix.png", dpi=160)
    plt.close(fig)


def main() -> None:
    parser = argparse.ArgumentParser(description="Train the reproducible TravelComfort bootstrap classifier.")
    parser.add_argument("--samples", type=int, default=6000)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--estimators", type=int, default=180)
    parser.add_argument("--feedback-json", type=Path, help="JSON array exported from tm_trip_comfort_feedback")
    args = parser.parse_args()
    bundle, metrics, curves, matrix = train(args.samples, args.seed, args.estimators, load_feedback(args.feedback_json))
    model_path = PYTHON_ROOT / "models" / "travel-comfort-v1.joblib"
    evidence_dir = REPO_ROOT / "docs" / "ai" / "evidence"
    model_path.parent.mkdir(parents=True, exist_ok=True)
    evidence_dir.mkdir(parents=True, exist_ok=True)
    joblib.dump(bundle, model_path)
    metrics_json = json.dumps(metrics, ensure_ascii=False, indent=2)
    (evidence_dir / "travel-comfort-metrics.json").write_text(metrics_json, encoding="utf-8")
    (REPO_ROOT / "frontend" / "src" / "data" / "travelComfortMetrics.json").write_text(metrics_json, encoding="utf-8")
    plots(metrics, curves, matrix, evidence_dir)
    print(json.dumps({"model": str(model_path), "accuracy": metrics["accuracy"], "macro_f1": metrics["macro_f1"]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
