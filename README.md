# Travel Mind

Travel Mind is a full-stack intelligent travel planning system with a consumer travel application, a separate operations console, Java trip planning, MySQL persistence, and local FastAPI AI services.

## Tech Stack

- Backend: Java 17, Spring Boot 3, Maven multi-module, MyBatis-Plus, Redis, Sa-Token.
- Frontend: Vue 3 + Vite.
- Python AI: FastAPI, Python 3.10/3.12, self-trained TravelRisk-YOLO and TravelComfort.
- Database: MySQL 8.0 with Redis 7 and Qdrant 1.15 for private travel-memory vectors.

## Project Structure

- `app`, `common`, `modules`: Spring Boot backend modules.
- `frontend`: Vue 3 application shell with router, API client, menu, and env config.
- `python-ai`: FastAPI service with health, image detection, trip comfort scoring, and text analysis endpoints.
- `app/src/main/resources/db`: Flyway schema migrations and development-only seed data.
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
npm ci
npm test
npm run dev
```

Vite runs on `http://localhost:5173` by default.

- Consumer application: `http://localhost:5173/`
- Consumer login: `http://localhost:5173/login`
- Admin console: `http://localhost:5173/admin`
- Admin login: `http://localhost:5173/admin/login`

The dev profile creates BCrypt-backed demo accounts on first startup. Override their passwords with `TRAVELMIND_DEMO_PASSWORD` and `TRAVELMIND_ADMIN_PASSWORD` before startup.

Login returns a signed JWT in the existing `tokenValue` field. The browser keeps it in session storage and sends it in the `Authorization` header. Set `JWT_SECRET` to a random 32+ byte value outside local development; the production profile has no fallback. `JWT_TTL_SECONDS` defaults to 30 days. Logout discards the JWT on the client: there is intentionally no refresh-token or server-side blacklist flow.

## Python AI

```bash
cd python-ai
python -m venv .venv
.venv/Scripts/python.exe -m pip install -r requirements.txt
.venv/Scripts/python.exe -m pytest
.venv/Scripts/python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 19080
```

Health check: `GET http://localhost:19080/health`.

The response distinguishes service liveness from model readiness for TravelRisk-YOLO, TravelComfort, the memory embedding model, and Qdrant. Missing optional models are reported as degraded together with the active fallback path.

AI endpoints:

- Internal-only vision, trip, content and memory APIs; every endpoint requires the Java-to-Python service token.

Java exposes matching authenticated user endpoints under `/api/user/ai/*` and stores results in `tm_ai_analysis_record`.

The bundled models are `python-ai/models/travel-risk-yolo-best.pt` and `python-ai/models/travel-comfort-v1.joblib`. Their training reports and evidence are under `docs/ai`.
Travel-memory retrieval uses `BAAI/bge-small-zh-v1.5` (about 100 MB on first download) and the fixed
512-dimensional cosine collection `travel_memory_v1`. Missing model/Qdrant is reported as unavailable, not replaced
with fake vectors.

## Configuration

Copy `.env.example` to `.env` and set local values for MySQL, Redis, map/content providers, large-model access, and the Python AI base URL. Java and Python both load this file. Frontend settings are documented in `frontend/.env.example`.

For production, fill every required secret and the one-time bootstrap administrator in `.env`, then run `docker compose -f compose.prod.yml up -d --build`. Only the Nginx frontend is exposed; MySQL, Redis, Qdrant, Java and Python remain on the internal network. Flyway migrates the database and creates the first administrator only when no active administrator exists.

## Delivery Documents

- Final TODO workflow: `docs/project-todo-workflow.md`.
- Dual-portal product boundary: `docs/design/dual-portal-product.md`.
- Backend API: `docs/api/backend-api.md`.
- Java-Python API: `docs/api/java-python-api.md`.
- Test report: `docs/test/test-report.md`.
- Deployment guide: `docs/deploy/deployment-guide.md`.
- Release notes: `docs/deploy/release-notes-v1.0.0.md`.

## License

This project keeps the original GPL-2.0 license file.
