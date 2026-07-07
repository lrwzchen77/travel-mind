# Java-Python AI API

Java backend calls the local FastAPI service through `PYTHON_AI_BASE_URL` (default `http://localhost:19080`). Python responses use the shared envelope:

```json
{"code":0,"message":"success","data":{}}
```

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

Each Java endpoint calls Python, stores an audit row in `tm_ai_analysis_record`, and returns a `PythonAiCallResult`. Python failures are stored with `status=failed` and do not block trip planning.
