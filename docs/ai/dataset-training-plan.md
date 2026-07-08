# TravelRisk-YOLO Dataset Acquisition And Training Plan

## Purpose

This plan records the dataset acquisition and model training direction for the Travel Mind short-semester project. The goal is to add a self-trained deep learning component rather than relying only on a pre-trained model or rule fallback.

The model target is:

```text
TravelRisk-YOLO: a lightweight visual model for travel scene recognition and basic travel risk detection.
```

## Target Scope

Use a small self-built image dataset to train a lightweight YOLOv8 classification model. The first version focuses on six classes:

| Class | Meaning | Business Use |
| --- | --- | --- |
| `scenic_spot` | scenic spots and natural attractions | enrich attraction profiles |
| `restaurant_food` | restaurants and food scenes | enrich restaurant and food recommendations |
| `hotel_room` | hotel rooms and accommodation scenes | support hotel profile analysis |
| `transport_station` | railway stations, airports, terminals, bus stations | identify transfer and transport contexts |
| `crowded_scene` | crowded tourist or public scenes | generate crowd risk hints |
| `low_light_scene` | night or low-light travel scenes | generate night travel safety hints |

`rainy_or_wet`, `queue`, and `stairs` are useful future classes, but they are excluded from the first version because image sources and labeling standards are less stable.

## Dataset Strategy

The project uses **crawler-collected images as the main dataset source** because self-collection is a project bonus point and better matches the travel domain. Public datasets can be cited or used as limited reference material.

Recommended sources:

- Image crawler: Bing Images, Baidu Images, Unsplash, Pexels, or other legal public image search sources.
- Alibaba Tianchi: optional public dataset reference or supplementary source if a suitable scene/travel/crowd dataset is available.
- Manual samples: a small number of manually downloaded or team-provided images for demonstration and testing.
- Large public datasets: only cite or sample from them when license and usage are clear; do not commit large raw datasets to the repository.

Recommended crawler keywords:

```text
西湖 景点 游客
杭州 景区 风景
杭州 美食 餐厅
酒店 房间 民宿
高铁站 候车厅
机场 候机厅
景区 人多 拥挤
夜景 景区 夜间 出行
```

## Dataset Size

Keep the dataset small enough for course delivery and repeatable training:

```text
50-100 images per class
300-600 images total
70% train, 20% validation, 10% test
```

The repository should include only a small `samples/` folder and metadata. Full raw datasets and trained weights should stay outside Git or be stored through an external release/archive.

## Dataset Layout

Use YOLOv8 classification layout:

```text
datasets/travel-risk-yolo/
  README.md
  data.yaml
  samples/
  images/
    train/
      scenic_spot/
      restaurant_food/
      hotel_room/
      transport_station/
      crowded_scene/
      low_light_scene/
    val/
      ...
    test/
      ...
```

Raw downloads should be stored separately before cleaning:

```text
datasets/raw/travel-risk-crawl/
```

## Collection And Cleaning Workflow

1. Crawl image URLs by class keyword and save source metadata.
2. Remove failed downloads, tiny images, duplicates, watermarked images, and unrelated images.
3. Manually review each class folder to keep labels consistent.
4. Split images into train, validation, and test folders.
5. Generate a dataset report with image counts, class balance, examples, and known limitations.

The crawler should respect rate limits, avoid login-only/private content, and record source URLs for traceability.

## Training Plan

Use YOLOv8n classification as the first training target:

```powershell
pip install ultralytics
yolo classify train model=yolov8n-cls.pt data=datasets/travel-risk-yolo/images epochs=30 imgsz=224 batch=16
```

Expected outputs:

```text
runs/classify/train/
  weights/best.pt
  results.csv
  confusion_matrix.png
  results.png
```

The trained model is then configured through:

```powershell
$env:TRAVEL_MIND_YOLO_MODEL="D:\models\travel-risk-yolo\best.pt"
```

## System Integration

The existing Python endpoint remains the integration point:

```text
POST /api/vision/detect
```

When `TRAVEL_MIND_YOLO_MODEL` is configured, Python AI loads the trained model and returns:

- `model_mode`: `trained_yolo`
- predicted labels and confidence scores
- travel scene tags
- risk hints for `crowded_scene` and `low_light_scene`
- image summary

If the model is missing or loading fails, the service keeps the existing rule fallback so trip planning is not blocked.

## Evaluation Criteria

Minimum project evidence:

- dataset report with class counts and sample images
- training command and parameters
- training result screenshots or exported metrics
- confusion matrix
- at least six test images across the six classes
- frontend or API demo showing trained model output

Suggested success target:

```text
validation accuracy >= 70%
test accuracy >= 65%
clear risk hints for crowded_scene and low_light_scene
```

Because this is a small course dataset, the report should describe limitations honestly instead of claiming production-grade accuracy.

## Documentation Deliverables

Add or update:

```text
docs/ai/dataset-training-plan.md
docs/ai/dataset-report.md
docs/ai/training-report.md
docs/api/java-python-api.md
python-ai/README.md
```

Recommended scripts for the implementation phase:

```text
python-ai/scripts/crawl_images.py
python-ai/scripts/prepare_dataset.py
python-ai/scripts/train_yolo.py
python-ai/scripts/predict_yolo.py
```

## Final Recommendation

Use a self-crawled small dataset as the main story, cite Alibaba Tianchi or other public datasets as optional reference, train a YOLOv8n classification model with six travel/risk classes, and connect the trained model to the existing image detection endpoint. This gives the project a clear self-trained deep learning component while keeping the workload realistic.
