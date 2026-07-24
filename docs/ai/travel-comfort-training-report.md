# TravelComfort v1 训练报告

## 目标与边界

TravelComfort 对行程的实际体感进行三分类：`relaxed`（偏松）、`balanced`（正合适）、`intense`（太赶）。首版模型用于证明从训练、部署到真实反馈采集的完整链路，不把模拟数据描述成用户数据。

## 数据与特征

- 数据：固定种子 `42` 生成 6,000 条可复现旅行场景，训练集 4,500 条，独立测试集 1,500 条。
- 标签：由公开的疲劳度生成规则产生，并加入固定分布噪声以避免模型只复刻硬阈值。
- 12 个特征：天数、景点总数、日均景点、单日最多景点、密集日、换乘日、跨城次数、不利天气日、轻松偏好、日均预算、预算压力、交通压力。
- 局限：当前没有路线距离和换乘时长；首版测试结果只代表模拟分布，不代表真实人群泛化能力。

## 模型与结果

模型为 scikit-learn `GradientBoostingClassifier`：180 棵树、学习率 0.05、最大深度 3、叶节点最少 12 条样本、subsample 0.9。

| 指标 | 结果 |
| --- | ---: |
| Accuracy | 83.13% |
| Macro-F1 | 81.64% |
| 测试集正确数 | 1,247 / 1,500 |
| 偏松 F1 | 76.47% |
| 正合适 F1 | 78.61% |
| 太赶 F1 | 89.82% |

证据文件位于 `docs/ai/evidence/`：指标 JSON、训练曲线和混淆矩阵。管理端直接读取同一份指标快照及图片，不维护第二套手写数字。

## 部署与降级

模型产物为 `python-ai/models/travel-comfort-v1.joblib`。FastAPI 的 `POST /api/trip/evaluate` 返回模型版本、三类概率、置信度和特征快照；模型缺失、版本不兼容或推理失败时保留确定性规则结果，出发前风险建议不会中断。

Java 继续复用现有行程评估记录，并提供：

- `GET /api/user/ai/trip/{tripId}/comfort/feedback`
- `POST /api/user/ai/trip/{tripId}/comfort/feedback`
- `GET /api/admin/ai/travel-comfort/feedback/stats`

反馈只能在行程结束后提交。服务端保存实际标签和当时的完整预测快照，确保后续训练能够还原输入，且不相信浏览器自行上传的特征。

## 复现与反馈训练

```powershell
cd python-ai
.\.venv\Scripts\python.exe scripts\train_travel_comfort.py
```

把 `tm_trip_comfort_feedback` 的 `actual_label`、`prediction_json` 导出为 JSON 数组后，可将真实反馈仅追加到训练集；固定模拟测试集仍保持独立：

```powershell
.\.venv\Scripts\python.exe scripts\train_travel_comfort.py --feedback-json .\feedback.json
```

每次发布新模型都必须同步版本、指标快照和管理端证据。真实反馈规模不足或类别失衡时，不应宣称个性化效果提升。
