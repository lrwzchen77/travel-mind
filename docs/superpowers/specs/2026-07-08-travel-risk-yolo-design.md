# TravelRisk-YOLO Dataset And Training Design

## Summary

Travel Mind will add a self-trained deep learning feature named TravelRisk-YOLO. The feature uses a small crawler-collected image dataset to train a lightweight YOLOv8 classification model for travel scene recognition and basic travel risk detection.

## Goals

- Show a real dataset acquisition, cleaning, training, evaluation, and integration workflow.
- Use self-crawled images as the main dataset source because crawler-based acquisition is a project bonus point.
- Keep the model small and repeatable for short-semester delivery.
- Integrate the trained model into the existing Python AI `POST /api/vision/detect` endpoint.

## Classes

The first version uses six classes:

```text
scenic_spot
restaurant_food
hotel_room
transport_station
crowded_scene
low_light_scene
```

These classes combine the travel scene goal and risk recognition goal. Harder risk classes such as `rainy_or_wet`, `queue`, and `stairs` are deferred because they require more careful data sourcing and labeling.

## Dataset Acquisition

The dataset will be built mainly through image crawling from public image search or open image sources. Alibaba Tianchi may be cited or used as supplementary public dataset material if a suitable scene or crowd dataset is found.

The crawler will collect URLs and metadata, download candidate images, and store raw files outside the cleaned training split. The team will then remove duplicate, broken, tiny, watermarked, private, or unrelated images.

Target size:

```text
50-100 images per class
300-600 images total
70% train, 20% validation, 10% test
```

## Training

The first implementation should use YOLOv8n classification:

```powershell
yolo classify train model=yolov8n-cls.pt data=datasets/travel-risk-yolo/images epochs=30 imgsz=224 batch=16
```

The expected training artifact is `best.pt`, configured through `TRAVEL_MIND_YOLO_MODEL`. Large datasets and model weights should not be committed to Git.

## Integration

Python AI loads the trained model when `TRAVEL_MIND_YOLO_MODEL` is set. The existing `/api/vision/detect` response shape stays stable and adds trained model metadata through `model_mode=trained_yolo`.

Risk classes map to business hints:

- `crowded_scene`: crowd and queue timing advice.
- `low_light_scene`: night travel and return transportation advice.

If the model is unavailable, the existing rule fallback remains active.

## Evaluation

The project should document dataset counts, training parameters, validation results, confusion matrix, sample predictions, and limitations. Suggested success target is validation accuracy of at least 70% and test accuracy of at least 65%.

## Documentation

The main project-facing plan is recorded in `docs/ai/dataset-training-plan.md`. Later implementation should add dataset and training reports under `docs/ai/`.
