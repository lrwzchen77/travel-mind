# Phase 4 Python AI Integration Design

## Goal

Complete Phase 4 by adding local FastAPI AI capabilities and wiring them into the Spring Boot backend, MySQL analysis records, trip review, and Vue pages without making the demo dependent on GPU, model downloads, or external services.

## Approach

Use the runnable-first path. Python exposes three stable JSON APIs:

- `POST /api/vision/detect` for image URL or uploaded image analysis.
- `POST /api/trip/evaluate` for rule-based trip comfort scoring.
- `POST /api/content/analyze` for travel text sentiment and keyword analysis.

The image endpoint includes a YOLO-compatible boundary but defaults to deterministic lightweight analysis when no model is configured. This satisfies the local AI contract while keeping Windows/Docker development reliable.

## Python Service

The FastAPI app remains independent from MySQL. It owns DTO validation, deterministic scoring/extraction rules, and response formatting. All endpoints return:

```json
{"code":0,"message":"success","data":{...}}
```

Image detection accepts multipart files or JSON with `image_url`, plus optional `city` and `resource_type`. It returns labels, confidence scores, scene tags, summary, risks, and `model_mode` (`rule` or `yolo`). Trip evaluation accepts trip days/items/preferences/budget and returns overall score, risk level, daily risk items, and suggestions. Text analysis accepts text/city/attraction/language and returns sentiment, keywords, positive highlights, negative warnings, and suitable traveler types.

## Java Integration

The backend adds `travelmind.python-ai.base-url` and timeout configuration. A Java HTTP client posts JSON to Python and gracefully returns fallback failure data when Python is unavailable. Controller endpoints under `/api/ai/*` expose the three capabilities to Vue:

- `POST /api/ai/vision/detect`
- `POST /api/ai/trip/evaluate`
- `POST /api/ai/content/analyze`

Every successful or failed Java-side call inserts a row into `tm_ai_analysis_record` with `analysis_type`, target metadata, request summary, result summary, `result_json`, and status.

Trip review is extended to call comfort scoring after a plan is saved. Python AI failures never block trip planning; fallback review text is persisted as an AI analysis record.

## Frontend

Add a practical AI Lab page for manual image detection, trip scoring, and text analysis. Trip detail fetches and displays the latest comfort score for the saved trip. The existing AI Records CRUD page remains the audit/history view.

## Testing

Python uses `pytest` + `httpx.ASGITransport` for all three endpoints. Java unit tests cover request serialization, fallback behavior, AI record persistence, and review integration using lightweight fake clients or in-memory JDBC where useful. Frontend tests cover API wrappers and route/menu registration. Final verification must run Python tests, Maven tests/build, frontend tests/build, and smoke tests across Python + Java + Vue.

## Constraints

Do not make Python access MySQL directly. Do not require GPU. Do not block trip planning if Python is down. Do not train a model without documented data. Keep the implementation close to existing project patterns and avoid unrelated refactors.
