# Deployment Guide

## Local Dependencies

- JDK 17.
- Maven 3.9+.
- Node.js 20+.
- Python 3.10 by default; Python 3.12 is allowed when dependencies are stable.
- MySQL 8.0 or 5.7-compatible schema.
- Redis 7.
- Qdrant 1.15 (provided by Compose).
- Docker Desktop for local MySQL/Redis containers.

## Database

Start MySQL 8.0 and Redis 7. On a fresh volume, Compose runs the SQL scripts automatically:

```bash
docker compose up -d --wait
docker exec travel-mind-mysql mysql -uroot -Nse "SELECT VERSION()"
docker exec travel-mind-redis redis-cli ping
curl http://localhost:6333/healthz
```

## Backend

Copy `.env.example` to the ignored `.env` file, configure credentials, then package and run. Spring Boot imports `.env` automatically:

```bash
mvn -pl app -am package -DskipTests
java -jar app/target/app-0.0.1-SNAPSHOT.jar
```

Health check:

```bash
curl http://localhost:8080/health
```

## Python AI

```bash
cd python-ai
.venv/Scripts/python.exe -m pip install -r requirements.txt
.venv/Scripts/python.exe -m pytest -q
.venv/Scripts/python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 19080
```

The bundled self-trained model is configured as `python-ai/models/travel-risk-yolo-best.pt`. Without it, deterministic rule fallback is used.

Private travel-memory indexing also requires `MEMORY_SERVICE_TOKEN`, `MEMORY_SCOPE_SECRET`, `QDRANT_URL`, and
`BAAI/bge-small-zh-v1.5`. The embedding model is downloaded on first use (roughly 100 MB). Production must set
`ENVIRONMENT=prod`, a unique internal token, and a 32+ byte scope secret; there is no production fallback.

## Frontend

```bash
cd frontend
npm install
npm test
npm run build
npm run dev -- --host 127.0.0.1
```

Open `http://localhost:5173`.

## Release Verification

Before delivery, run:

```bash
python-ai/.venv/Scripts/python.exe -m pytest -q
mvn test
mvn -pl app -am package -DskipTests
cd frontend && npm test
cd frontend && npm run build
```

Then smoke test backend health, Python health, frontend routes, CRUD, trip planning, trip history, Java-Python AI endpoints, and failure fallbacks.
