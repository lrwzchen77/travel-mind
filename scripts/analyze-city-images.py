"""Run the bundled travel-risk classifier over downloaded city covers."""

import json
import os
from pathlib import Path

from ultralytics import YOLO


ROOT = Path(__file__).resolve().parents[1]
IMAGE_DIR = ROOT / "frontend" / "public" / "city-images"
OUTPUT = ROOT / "frontend" / "src" / "data" / "cityVisionInsights.json"
MODEL = ROOT / os.getenv(
    "TRAVEL_MIND_YOLO_MODEL",
    "python-ai/models/travel-risk-yolo-best.pt",
)


def main() -> None:
    images = sorted(IMAGE_DIR.glob("*.jpg"))
    model = YOLO(str(MODEL))
    predictions = model.predict(source=[str(image) for image in images], verbose=False)
    insights = {}
    for image, prediction in zip(images, predictions, strict=True):
        class_id = int(prediction.probs.top1)
        insights[image.stem] = {
            "label": str(prediction.names[class_id]),
            "confidence": round(float(prediction.probs.top1conf), 4),
        }
    OUTPUT.write_text(
        json.dumps(insights, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
