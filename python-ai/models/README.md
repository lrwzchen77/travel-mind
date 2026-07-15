# TravelRisk-YOLO Model

`travel-risk-yolo-best.pt` is the self-trained YOLOv8n classification model used by `POST /api/vision/detect`.

- Classes: `scenic_spot`, `restaurant_food`, `hotel_room`, `transport_station`, `crowded_scene`, `low_light_scene`.
- Dataset: 946 images across train, validation, and test splits.
- Validation accuracy: 0.9681.
- Test accuracy: 0.9394.
- Runtime: CPU-compatible through Ultralytics and PyTorch.

Training and evaluation evidence is stored under `docs/ai/`. Set `TRAVEL_MIND_YOLO_MODEL=python-ai/models/travel-risk-yolo-best.pt`; clear it only when deliberately testing rule fallback.
