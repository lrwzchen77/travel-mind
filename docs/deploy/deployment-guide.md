# Deployment Guide

## Local Dependencies

- JDK 17.
- Maven 3.9+.
- Node.js 20+.
- Python 3.10 by default; Python 3.12 is allowed when dependencies are stable.
- MySQL 8.0 or 5.7-compatible schema.
- Redis 7.
- Docker Desktop for local MySQL/Redis containers.

## Database

Run MySQL and Redis, then initialize the database:

```bash
docker start travel-mind-mysql travel-mind-redis
docker exec -i travel-mind-mysql mysql -uroot < sql/001_create_database.sql
docker exec -i travel-mind-mysql mysql -uroot travelmind < sql/002_phase2_crud_schema.sql
docker exec -i travel-mind-mysql mysql -uroot travelmind < sql/003_phase2_seed_data.sql
```

## Backend

Configure environment values from `.env.example`, then package and run:

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

Set `TRAVEL_MIND_YOLO_MODEL` only when an optional local YOLO model and `ultralytics` are installed. Without it, deterministic rule fallback is used.

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
