# Travel Mind Python AI

FastAPI service for local Travel Mind AI capabilities. The root `.env` file is loaded automatically.

```bash
python -m venv .venv
.venv/Scripts/python.exe -m pip install -r requirements.txt
.venv/Scripts/python.exe -m pytest
.venv/Scripts/python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 19080
```

Endpoints:

- `GET /health`
- `POST /api/vision/detect`
- `POST /api/trip/evaluate`
- `POST /api/content/analyze`
- `POST /api/memory/analyze` (Java-validated trip memory items only)

The bundled `models/travel-risk-yolo-best.pt` model is enabled by the root `.env.example` configuration. Clear `TRAVEL_MIND_YOLO_MODEL` to exercise deterministic rule fallback.
