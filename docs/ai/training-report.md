# 训练报告（TravelRisk-YOLO）

> 对应验收 **M6–M10、M17、N8**。  
> **当前版本：双来源合并后重训**（2026-07-13）。  
> 元数据：`artifacts/train_meta.json`、`artifacts/metrics.json`、`artifacts/dataset_stats.json`。

## 1. 环境

| 项 | 值 |
| --- | --- |
| Python | 3.12.10 |
| 虚拟环境 | `D:\Work\venvs\travel-crawl-train` |
| 设备 | **CPU**（Intel Core Ultra 5 245KF） |
| torch | 2.5.1+cpu |
| ultralytics | 8.4.93 |
| 本轮训练耗时 | **约 5.1 分钟**（20 epoch） |

## 2. 数据规模（本轮训练所依据）

| 集合 | 数量 | 来源构成 |
| --- | ---: | --- |
| train | 659 | 爬虫 + 公开抽样合并后 70% |
| val | 188 | 同上 20% |
| test | 99 | 同上 10% |
| **合计** | **946** | 爬虫 **766** + 公开 **180** |

划分 70/20/10，seed=42。详见 `reports/dataset-report.md`。

## 3. 超参

| 项 | 值 |
| --- | --- |
| 模型 | yolov8n-cls（预训练，nc=6） |
| epochs | 20 |
| imgsz | 224 |
| batch | 8 |
| seed | 42 |
| device | cpu |
| data | `datasets/travel-risk-yolo/images` |
| 权重 | `artifacts/best.pt` |

```powershell
$py = "D:\Work\venvs\travel-crawl-train\Scripts\python.exe"
cd D:\xxqxm\爬虫+训练
& $py scripts/08_download_sample_public.py --per-class 30 --skip-places
& $py scripts/09_merge_and_resplit.py --wipe-out
& $py scripts/04_train_cls.py --epochs 20 --batch 8 --imgsz 224 --seed 42 --device cpu
& $py scripts/05_eval_cls.py --weights artifacts/best.pt --split both --device cpu
```

## 4. 指标（双来源 · 独立评测）

| 指标 | 数值 | 样本数 |
| --- | ---: | ---: |
| **val accuracy** | **0.9681** | 188 |
| **test accuracy** | **0.9394** | 99 |

- Should：val≥0.65、test≥0.60 → **通过**  
- 训练日志末 epoch top1 ≈ 0.957（独立 val 用 best.pt 为 **0.9681**）

### 分阶段对照

| 项 | 仅小爬虫集 | 爬虫扩集 | **双来源（本轮）** |
| --- | ---: | ---: | ---: |
| 入集总量 | 438 | 766 | **946** |
| test n | 44 | 76 | **99** |
| val acc | 0.9545 | 0.9869 | **0.9681** |
| test acc | 1.0000 | 0.9605 | **0.9394** |

说明：混入公开集后 test 略降到 **0.94**，属于预期——分布变杂、test 更大，指标更可信；仍远超 Should 线。

### 各类 test 表现（摘要）

| 类 | precision | recall | f1 | support |
| --- | ---: | ---: | ---: | ---: |
| scenic_spot | 0.85 | 0.94 | 0.89 | 18 |
| restaurant_food | 1.00 | 1.00 | 1.00 | 16 |
| hotel_room | 1.00 | 0.94 | 0.97 | 18 |
| transport_station | 0.92 | 0.86 | 0.89 | 14 |
| crowded_scene | 0.94 | 1.00 | 0.97 | 15 |
| low_light_scene | 0.94 | 0.89 | 0.91 | 18 |

完整混淆矩阵与 per-class：`artifacts/metrics.json`。

## 5. 图表

| 文件 | 说明 |
| --- | --- |
| `reports/screenshots/results.png` | 训练曲线 |
| `reports/screenshots/confusion_matrix.png` | 训练期 val 混淆矩阵 |
| `reports/screenshots/confusion_matrix_test.png` | **独立 test 混淆矩阵** |
| `reports/screenshots/confusion_matrix_normalized.png` | 归一化矩阵 |

## 6. 六类样例预测（samples）

| 真实类 | 预测类 | 置信度 |
| --- | --- | --- |
| scenic_spot | scenic_spot | 0.9950 |
| restaurant_food | restaurant_food | 0.9999 |
| hotel_room | hotel_room | 0.9996 |
| transport_station | transport_station | 0.6485 |
| crowded_scene | crowded_scene | 0.9933 |
| low_light_scene | low_light_scene | 0.9975 |

六类均预测正确；`transport_station` 置信度相对较低，与公开弱映射 + 类内多样性一致。

## 7. 系统接入（P5）

| 项 | 值 |
| --- | --- |
| 权重路径 | `D:\xxqxm\爬虫+训练\artifacts\best.pt` |
| 环境变量 | `TRAVEL_MIND_YOLO_MODEL` |
| 服务 | `travel-mind/python-ai` · uvicorn `:19080` |
| 接口 | `POST /api/vision/detect` |
| 改动文件 | `python-ai/app/main.py`（优先 `probs` 分类，`model_mode=trained_yolo`） |

```powershell
$env:TRAVEL_MIND_YOLO_MODEL="D:\xxqxm\爬虫+训练\artifacts\best.pt"
cd D:\xxqxm\travel-mind\python-ai
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 19080
```

## 8. 局限与改进

- 公开抽样每类 30 张；transport 弱映射、hotel 用 bedroom 子集。  
- test=99 仍偏小；高分含 ImageNet 预训练贡献。  
- 可选下一步：Places365 类级子集替换 Intel 弱映射；人工清噪。

## 9. 结论（答辩可用）

在 **爬虫 766 + 公开 180 = 946 张** 六类数据上完成 YOLOv8n-cls 重训与独立评测：  
**val≈0.97、test≈0.94（n=99）**，权重 `artifacts/best.pt`；  
数据叙事为 **双来源**（搜索爬虫 + Food-101 / ExDark / ShanghaiTech / Indoor-67 / Intel），过程可复现。

## 10. 阶段检查

- [x] P3：`best.pt` + 超参（双来源重训）  
- [x] P4：metrics + 混淆矩阵 + 六类预测 + 局限  
- [x] P5：`trained_yolo` 接入路径不变（权重已覆盖）  
- [x] N8：公开集融合后重训并更新 metrics  
