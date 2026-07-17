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
- `POST /api/memory/index` (private BGE/Qdrant index or reindex)
- `POST /api/memory/query` (single-memory evidence-backed question)
- `POST /api/memory/delete` (delete one owner's memory vectors)

All `/api/memory/*` routes require `X-Internal-Service-Token`. The development default is only active when
`ENVIRONMENT`/`SPRING_PROFILES_ACTIVE` is not `prod`; production must set `MEMORY_SERVICE_TOKEN` explicitly.
Java sends an opaque HMAC owner scope, and every Qdrant query additionally filters by that scope and `memory_id`.

`sentence-transformers==5.1.2` loads `BAAI/bge-small-zh-v1.5` lazily. Its first run downloads roughly 100 MB.
If the package or model is unavailable, index/query returns HTTP 503; it never substitutes hash or random vectors.
Qdrant uses the fixed `travel_memory_v1` collection with 512-dimensional cosine vectors.
Java gives memory analysis/index/query a separate `PYTHON_MEMORY_TIMEOUT_MS` (default 120 seconds) because model cold
start and batches are slower than ordinary AI calls.

The bundled `models/travel-risk-yolo-best.pt` model is enabled by the root `.env.example` configuration. Clear `TRAVEL_MIND_YOLO_MODEL` to exercise deterministic rule fallback.
