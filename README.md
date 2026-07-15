# Travel Mind

Travel Mind is a full-stack intelligent travel planning system with Java trip planning, Vue management pages, MySQL persistence, and local FastAPI AI services.

## Tech Stack

- Backend: Java 17, Spring Boot 3, Maven multi-module, MyBatis-Plus, Redis, Sa-Token.
- Frontend: Vue 3 + Vite.
- Python AI: FastAPI, Python 3.10/3.12, self-trained TravelRisk-YOLO.
- Database: MySQL 8.0 with Redis 7.

## Project Structure

- `app`, `common`, `modules`: Spring Boot backend modules.
- `frontend`: Vue 3 application shell with router, API client, menu, and env config.
- `python-ai`: FastAPI service with health, image detection, trip comfort scoring, and text analysis endpoints.
- `sql`: MySQL initialization scripts.
- `docs`: design, API, agile, test, and deploy documents.

## Backend

```bash
mvn test
mvn -pl app -am package -DskipTests
mvn -pl app -am spring-boot:run
```

Health check: `GET http://localhost:8080/health`.

The backend automatically imports the ignored root `.env` file. Process environment variables still take precedence.

## Frontend

```bash
cd frontend
npm install
npm test
npm run dev
```

Vite runs on `http://localhost:5173` by default.

## Python AI

```bash
cd python-ai
python -m venv .venv
.venv/Scripts/python.exe -m pip install -r requirements.txt
.venv/Scripts/python.exe -m pytest
.venv/Scripts/python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 19080
```

Health check: `GET http://localhost:19080/health`.

AI endpoints:

- `POST http://localhost:19080/api/vision/detect`
- `POST http://localhost:19080/api/trip/evaluate`
- `POST http://localhost:19080/api/content/analyze`

Java exposes matching frontend endpoints under `/api/ai/*` and stores results in `tm_ai_analysis_record`.

The bundled model is `python-ai/models/travel-risk-yolo-best.pt`. Its dataset, training report, and metrics are under `docs/ai`.

## Configuration

Copy `.env.example` to `.env` and set local values for MySQL, Redis, map/content providers, large-model access, and the Python AI base URL. Java and Python both load this file. Frontend settings are documented in `frontend/.env.example`.

## Delivery Documents

- Final TODO workflow: `docs/project-todo-workflow.md`.
- Backend API: `docs/api/backend-api.md`.
- Java-Python API: `docs/api/java-python-api.md`.
- Test report: `docs/test/test-report.md`.
- Deployment guide: `docs/deploy/deployment-guide.md`.
- Release notes: `docs/deploy/release-notes-v1.0.0.md`.

## License

This project keeps the original GPL-2.0 license file.
