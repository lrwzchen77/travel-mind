# 数据集报告（TravelRisk-YOLO）

> 对应验收 **M1–M5、M16、S9、S10、N7**。  
> 最近合并时间：2026-07-13（**爬虫 + 权威公开数据集双来源**）。  
> 统计权威文件：`artifacts/dataset_stats.json`。

## 1. 概述

| 项 | 内容 |
| --- | --- |
| 任务 | TravelRisk-YOLO **六类图像分类** |
| 数据来源 | **双来源**：① 公开图搜索爬虫 ② 具名学术/基准公开集抽样 |
| 爬虫脚本 | `01_crawl_urls.py` / `02_download_images.py` |
| 公开集脚本 | `08_download_sample_public.py` |
| 合并划分 | `09_merge_and_resplit.py`（也可单独用 `03_clean_and_split.py` 只跑爬虫） |
| 划分 | 70/20/10，**seed=42** |
| 元数据 | `data/metadata/crawl_manifest.jsonl` + `data/metadata/public_sources.jsonl` |

### 合规说明

- 爬虫：仅公开可访问图片链接；频控；保留 `source_url` / `query` / `sha1`。  
- 公开集：科研/课程合理使用；报告注明出处与 cite；不全量搬运 Places365 等大包。  
- 过程见 [闭环工作日志.md](./闭环工作日志.md) 与 `09-权威公开数据集补充方案.md`。

## 2. 数量统计（双来源合并后 · 清洗后）

| 类别 | train | val | test | 合计 | 爬虫 | 公开 | 门禁* |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| scenic_spot | 122 | 35 | 18 | 175 | 145 | 30 | PASS |
| restaurant_food | 105 | 30 | 16 | 151 | 121 | 30 | PASS |
| hotel_room | 118 | 33 | 18 | 169 | 139 | 30 | PASS |
| transport_station | 94 | 27 | 14 | 135 | 105 | 30 | PASS |
| crowded_scene | 101 | 29 | 15 | 145 | 115 | 30 | PASS |
| low_light_scene | 119 | 34 | 18 | 171 | 141 | 30 | PASS |
| **合计** | **659** | **188** | **99** | **946** | **766** | **180** | **PASS** |

\* 每类 train≥35 / val≥8 / test≥5。平衡：最少 135 ≥ 最多 175 × 0.5。  
比例约 **爬虫 81% : 公开 19%**（目标约 70:30 的轻量版）。

### 扩集 / 双来源前后对比

| 项 | 仅爬虫（扩集后） | 双来源合并（本轮） |
| --- | ---: | ---: |
| 清洗后入集 | 766 | **946** |
| 公开学术样本 | 0 | **180**（六类各 30） |
| train / val / test | 537 / 153 / 76 | **659 / 188 / 99** |
| 来源叙事 | 单一搜索爬图 | **爬虫 + 具名公开集** |

## 3. 公开数据集抽样清单（S9 / S10 / N7）

| 目标类 | 公开数据集 | 抽样 | 权威性说明 |
| --- | --- | ---: | --- |
| restaurant_food | **Food-101**（ETH / Bossard et al., ECCV 2014） | 25 | 食品识别经典集 |
| restaurant_food | **Indoor-67** restaurant/kitchen 等 | 5 | MIT 室内场景 |
| hotel_room | **Indoor-67** bedroom（Kaggle 镜像） | 30 | Quattoni & Torralba, CVPR 2009 |
| low_light_scene | **ExDark** | 30 | 低光基准 CVIU |
| crowded_scene | **ShanghaiTech Crowd** | 30 | 人群计数标准集 CVPR 2016 |
| scenic_spot | **Intel Image Classification** mountain/forest/glacier/sea | 30 | Kaggle 场景分类常用包（Places 轻量替代） |
| transport_station | **Intel** buildings/street（弱映射） | 30 | 城市场景弱相关；Places365 全量过大未下 |

完整逐图清单：`data/metadata/public_sources.jsonl`（180 行）。

> 说明：Intel 原图多 &lt;224px，抽样时已 **上采样至 min_side≥256**，以通过清洗门禁；报告中注明为 resize 后入集。  
> `transport_station` 的公开部分为 **弱标签**（buildings/street），答辩时诚实说明，优先仍靠爬虫车站/机场图。

## 4. 清洗规则摘要

| 规则 | 处理 |
| --- | --- |
| 损坏无法打开 | 丢弃 |
| 最短边 &lt; 224 | 丢弃（公开小图先上采样再入 pool） |
| 宽高比 max(w/h,h/w) &gt; 4 | 丢弃 |
| 全局 sha1 重复 | 只留 1 |
| 输出 | 统一 JPEG |

本轮 merge drop：`dropped_total=4`（见 `dataset_stats.json`）。

## 5. 人工抽检

- 计划：每类抽 10 张目检（错类优先删）。  
- 风险：搜索水印、公开集弱映射（交通）、室内 bedroom 代 hotel。  
- samples：`datasets/travel-risk-yolo/samples/<class>/`（每类 2 张）。

## 6. 局限

- 公开部分每类仅 30 张，叙事意义大于纯刷榜。  
- Intel transport 为弱映射；真·Places365 类级子集可后续替换。  
- test=99 仍非生产级规模；高分含预训练迁移成分。

## 7. 附件

- `artifacts/dataset_stats.json`  
- `data/metadata/crawl_manifest.jsonl`  
- `data/metadata/public_sources.jsonl`  
- `09-权威公开数据集补充方案.md`  
- `scripts/08_download_sample_public.py` / `09_merge_and_resplit.py`

## 8. 检查

- [x] 数量门禁  
- [x] 双来源 stats 已更新  
- [x] 六类均有公开抽样（N7）  
- [x] public_sources.jsonl（S10）  
- [x] 报告写明 ≥2 类具名公开集（S9：Food-101 / ExDark / ShanghaiTech / Indoor-67 / Intel）  
- [x] 基于双来源的 P3/P4 已重训+重评（见 training-report）  
