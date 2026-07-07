# Module Design

## Backend Modules

- `app`: REST controllers and Spring Boot launcher.
- `common/core`: shared response objects, exceptions, runtime setting keys, and common domain models.
- `common/json`: Jackson configuration, large-number serialization, and JSON utilities.
- `common/web`: CORS, trace ID, and web filters.
- `common/mybatis`: MyBatis-Plus base configuration.
- `common/redis`: Redis template and serializer configuration.
- `common/satoken`: Sa-Token authentication and route exclusion rules.
- `modules/resources`: CRUD registry, SQL builder, generic resource service, profile service, and trip history support.
- `modules/trip`: trip request DTOs, async task service, demo planner, persistence, reviewer, chat, Java-Python AI client, and AI record service.
- `modules/ai`: large-model prompt and text generation boundary.
- `modules/map`: map context boundary.
- `modules/content`: travel content collection and extraction boundary.

## Frontend Modules

- `src/api`: Axios clients for resources, trip planning, and AI endpoints.
- `src/layout`: application shell and menu.
- `src/router`: Vue route definitions.
- `src/views`: planning, history, detail, CRUD, AI Lab, and AI records views.

## Python AI Modules

- `app/main.py`: FastAPI app, DTOs, deterministic AI rules, optional YOLO boundary, and health endpoint.
- `tests`: pytest coverage for health and all Phase 4 AI endpoints.

## Integration Contracts

- Java-to-Python calls use `PYTHON_AI_BASE_URL` and timeout configuration.
- Python returns `{code,message,data}` envelopes.
- Java wraps Python outcomes in `PythonAiCallResult` and persists all outcomes.
