# System Architecture

Travel Mind is a separated full-stack travel planning system. The browser talks only to the Java backend; Java owns orchestration, persistence, and provider integration; Python AI exposes local AI capabilities through REST.

## Runtime Components

- `app`: Spring Boot entry module and REST controllers.
- `common`: shared response, JSON, web, MyBatis, Redis, and Sa-Token configuration.
- `modules/identity`: BCrypt account verification and user/admin role lookup.
- `modules/resources`: MySQL-backed resource CRUD, public discovery, and current-user profile/library services.
- `modules/trip`: async trip planning, persistence, review, chat, Java-Python AI client, and AI record persistence.
- `modules/ai`, `modules/map`, `modules/content`: large-model, map, and travel content integration boundaries.
- `frontend`: one Vue build with isolated consumer and admin route trees, layouts, sessions, and navigation.
- `python-ai`: FastAPI service for image detection, trip comfort scoring, and text analysis.
- `sql`: MySQL schema and seed data.

## Main Flow

```text
Vue consumer `/` ------> `/api/public/**` + `/api/user/**` --+
Vue admin `/admin` ----> `/api/admin/**` --------------------+-> shared domain services -> MySQL
                            -> Redis/session/cache
                            -> Large-model/map/content providers when configured
                            -> FastAPI Python AI through REST
```

## Failure Boundaries

- Python AI failures are saved to `tm_ai_analysis_record` and do not block trip planning.
- Missing external runtime settings make the backend use the MySQL Demo Planner.
- Python AI never accesses MySQL directly; Java persists all AI records.
- Consumer requests cannot select another `userId`; ownership comes from the Sa-Token session.
- Admin and consumer tokens are role-isolated in both directions.
