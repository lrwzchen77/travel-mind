# Java-Python AI API

Java backend calls the local FastAPI service through `PYTHON_AI_BASE_URL` (default `http://localhost:19080`). Python responses use the shared envelope:

```json
{"code":0,"message":"success","data":{}}
```

All `/api/memory/*` calls additionally require `X-Internal-Service-Token`. They accept only Java's opaque owner scope,
never a JWT or raw user ID. Qdrant reads are always filtered by both `owner_scope` and `memory_id`.

## Python Service Endpoints

### `POST /api/vision/detect`

Accepts JSON or multipart upload.

JSON request:

```json
{
  "image_url": "https://example.com/west-lake-night-food.jpg",
  "city": "Hangzhou",
  "resource_type": "attraction"
}
```

Response data:

```json
{
  "model_mode": "rule",
  "labels": [{"name": "travel_scene", "confidence": 0.91}],
  "scene_tags": ["travel_scene", "night_view"],
  "summary": "Hangzhou attraction image summary",
  "risk_hints": ["夜间出行注意返程交通和照明。"],
  "source": "image_url"
}
```

Set `TRAVEL_MIND_YOLO_MODEL` to enable the optional YOLO adapter. Without a model, the endpoint uses deterministic rule fallback.

### `POST /api/trip/evaluate`

Request:

```json
{
  "transportation": "公共交通",
  "budget": 3000,
  "preferences": ["轻松"],
  "days": [
    {"date": "2026-08-01", "city": "Hangzhou", "attractions": ["西湖", "灵隐寺"], "weather": "晴"}
  ]
}
```

Response data includes `comfort_score`, `risk_level`, `daily_risks`, and `suggestions`.

### `POST /api/content/analyze`

Request:

```json
{
  "text": "西湖风景很好，但是节假日排队很久。",
  "city": "杭州",
  "attraction_name": "西湖",
  "language": "zh"
}
```

Response data includes `sentiment`, `keywords`, `positive_highlights`, `negative_warnings`, and `suitable_traveler_types`.

## Java Frontend-Facing Endpoints

- `POST /api/ai/vision/detect`
- `POST /api/ai/trip/evaluate?targetType=trip_plan&targetId=9001`
- `POST /api/ai/content/analyze?targetType=travel_note&targetId=7001`
- `GET /api/ai/trip/{id}/comfort`
- `POST /api/user/memories/{memoryId}/index`
- `POST /api/user/memories/{memoryId}/ask` with `{"question":"哪家餐厅值得再去？","top_k":5}`
- `DELETE /api/user/memories/{memoryId}` deletes Qdrant vectors before MySQL data

The memory answer shape is evidence-first:

```json
{
  "answer": "根据这次旅行记录：第 1 天，西湖，上午游览西湖",
  "citations": [
    {"memoryItemId": 11, "sourceType": "trip_item", "sourceId": 81, "excerpt": "第 1 天，西湖，上午游览西湖"}
  ],
  "fallback": true
}
```

Java validates memory ownership before calling Python and revalidates every returned citation. Any foreign or altered
citation rejects the whole answer. `top_k` is capped at 10 and questions are limited to 500 characters.

Each Java endpoint calls Python, stores an audit row in `tm_ai_analysis_record`, and returns a `PythonAiCallResult`. Python failures are stored with `status=failed` and do not block trip planning.
