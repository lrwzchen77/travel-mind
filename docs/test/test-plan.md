# Test Plan

## Scope

Phase 5 verifies Travel Mind as a complete delivery package: Spring Boot backend, Vue frontend, FastAPI Python AI, MySQL, Redis, CRUD resources, trip planning, Java-Python REST integration, and failure fallbacks.

## Test Levels

- Unit tests: backend service logic, SQL builders, trip review, Java-Python client, Python deterministic AI rules, and frontend API wrappers.
- Controller/API tests: key Java AI controller endpoints and CRUD/list/detail flows.
- Integration smoke: running backend, frontend, Python AI, MySQL, and Redis together.
- Failure tests: Python AI unavailable, large-model provider unavailable, and non-blocking trip planning fallback.

## Commands

```bash
cd python-ai && .venv/Scripts/python.exe -m pytest -q
mvn test
mvn -pl app -am package -DskipTests
cd frontend && npm test
cd frontend && npm run build
```

## Manual Smoke Checklist

- `GET /health` returns backend healthy.
- `GET http://127.0.0.1:19080/health` returns Python healthy.
- Vue routes `/`, `/planning`, `/trip-history`, `/ai-lab`, `/ai-records`, and `/trip/{id}` return HTTP 200.
- CRUD creates, reads, updates, and deletes a demo city through `/api/cities`.
- Trip planning submits `/api/trip/plan`, polls `/api/trip/status/{taskId}`, and stores a plan visible through `/api/trip/history`.
- Java AI endpoints call Python and persist records in `tm_ai_analysis_record`.
- Stopping Python AI makes Java AI return a failed `PythonAiCallResult` without breaking trip planning.
