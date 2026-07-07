# System Architecture

Travel Mind is a separated full-stack travel planning system. The browser talks only to the Java backend; Java owns orchestration, persistence, and provider integration; Python AI exposes local AI capabilities through REST.

## Runtime Components

- `app`: Spring Boot entry module and REST controllers.
- `common`: shared response, JSON, web, MyBatis, Redis, and Sa-Token configuration.
- `modules/resources`: MySQL-backed CRUD and user profile services.
- `modules/trip`: async trip planning, persistence, review, chat, Java-Python AI client, and AI record persistence.
- `modules/ai`, `modules/map`, `modules/content`: large-model, map, and travel content integration boundaries.
- `frontend`: Vue 3 + Vite SPA with planning, history, resources, AI Lab, and AI records pages.
- `python-ai`: FastAPI service for image detection, trip comfort scoring, and text analysis.
- `sql`: MySQL schema and seed data.

## Main Flow

```text
Vue SPA -> Spring Boot REST -> Trip/CRUD services -> MySQL
                            -> Redis/session/cache
                            -> Large-model/map/content providers when configured
                            -> FastAPI Python AI through REST
```

## Failure Boundaries

- Python AI failures are saved to `tm_ai_analysis_record` and do not block trip planning.
- Missing external runtime settings make the backend use the MySQL Demo Planner.
- Python AI never accesses MySQL directly; Java persists all AI records.
