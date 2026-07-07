# Travel Mind Project TODO Workflow

## Project Positioning

Travel Mind is a full-stack intelligent travel planning system built from scratch by a four-person agile team.

The system must deliver:

- A Spring Boot + Vue business application.
- MySQL-backed traditional CRUD features.
- Large model API integration for travel planning.
- Local Python AI services connected through REST APIs.
- End-to-end operation across frontend, Java backend, Python AI service, and database.
- Clear agile process materials and Git history showing team collaboration.

## Locked Technical Stack

| Area | Required Stack | Boundary |
| --- | --- | --- |
| Backend | Java, Spring Boot | JDK must be 17 or lower. Use JDK 17 as the target. |
| Frontend | Vue | Use Vue 3 + Vite. Keep frontend and backend separated. |
| Python AI | Python + FastAPI | Use Python 3.10 by default. Python 3.12 is allowed only if dependencies are stable. |
| Database | MySQL | Use MySQL 8.0 by default. Keep SQL compatible with MySQL 5.7 where practical. |
| Integration | REST API | Java backend calls Python AI service through HTTP REST. |
| AI | Local AI + Large Model API | Local AI handles vision/scoring/NLP. Large model API handles travel planning and chat. |

## Team Roles

| Member | Role | Primary Scope |
| --- | --- | --- |
| Wang-ke-li | Team Lead, Backend Architect, AI Orchestration Lead | Project planning, architecture, backend core, large model agents, Java-Python integration, final system integration. |
| Chen Wenzhe | Frontend Engineer | Vue application, interaction design, travel planning pages, CRUD pages, AI result pages, frontend integration. |
| Zhu Qicheng | Python AI Engineer | FastAPI service, YOLO detection, trip comfort scoring, travel text analysis, AI API contracts. |
| Zhang Shuai | Database, CRUD, Test, Delivery Engineer | MySQL schema, CRUD services, seed data, test cases, agile documents, deployment documents. |

## Git Collaboration Rules

Use the following author identities:

```text
Wang-ke-li <Wang-ke-li@users.noreply.gitee.com>
Chen Wenzhe <chen-wenzhe@example.local>
Zhu Qicheng <zhu-qicheng@example.local>
Zhang Shuai <zhang-shuai@example.local>
```

Branch model:

```text
main
develop
feature/backend-core
feature/frontend-vue
feature/python-ai
feature/database-crud
feature/docs-test
```

Commit distribution target:

| Member | Target Share | Notes |
| --- | ---: | --- |
| Wang-ke-li | 35%-45% | More commits are expected because the role includes management, architecture, backend core, AI orchestration, and final integration. |
| Chen Wenzhe | 20%-25% | Frontend pages, frontend state, API integration, visual result display. |
| Zhu Qicheng | 18%-22% | Python AI service, model wrappers, algorithm outputs, AI service tests. |
| Zhang Shuai | 18%-22% | Database, CRUD, test reports, delivery materials. |

Commit message format:

```text
type(scope): concise action
```

Allowed types:

```text
feat, fix, docs, test, chore, style, ci
```

Example Wang-ke-li commits:

```text
docs(project): define product scope and sprint plan
feat(backend): add unified response and exception handling
feat(trip): implement async travel planning task workflow
feat(ai): add structured large model agent orchestration
feat(integration): connect Java backend with Python AI service
chore(release): complete final integration checklist
```

## Five-Phase TODO Workflow

## Phase 1: Project Initialization And Architecture

Goal: establish a clean full-stack system foundation that satisfies the locked technical stack.

Owner emphasis:

- Wang-ke-li leads project scope, architecture, backend foundation, and interface rules.
- Chen Wenzhe initializes Vue.
- Zhu Qicheng initializes Python AI.
- Zhang Shuai initializes database and document structure.

### TODO

- [x] Create root project structure:
  - `backend` or Maven root modules.
  - `frontend`.
  - `python-ai`.
  - `sql`.
  - `docs`.
- [x] Lock Java target to JDK 17.
- [x] Use Spring Boot 3.x.
- [x] Configure Maven multi-module backend structure.
- [x] Add backend base modules:
  - common response.
  - exception handling.
  - validation.
  - logging.
  - CORS.
  - MyBatis-Plus base support.
- [x] Create Vue 3 + Vite project.
- [x] Add frontend base layout:
  - router.
  - API client.
  - layout shell.
  - menu structure.
  - environment config.
- [x] Create Python FastAPI project.
- [x] Add Python health check endpoint:
  - `GET /health`.
- [x] Create MySQL database initialization folder.
- [x] Create base documents:
  - `docs/design`.
  - `docs/api`.
  - `docs/agile`.
  - `docs/test`.
  - `docs/deploy`.
- [x] Define API naming rules.
- [x] Define Java-Python REST response format.
- [x] Define Git branch and author workflow.

### Done Criteria

- [x] Backend starts with JDK 17.
- [x] Frontend starts with Vite.
- [x] Python service starts with FastAPI.
- [x] MySQL connection config exists.
- [x] Basic documents exist.
- [x] Team branch and commit rules are documented.

### Out Of Scope

- Do not implement travel planning logic in this phase.
- Do not implement AI algorithms in this phase.
- Do not build complete frontend pages in this phase.

## Phase 2: Traditional Business CRUD And Travel Resource Library

Goal: build enough traditional business features to make the system a real management application, not only an AI demo.

Business boundary:

The CRUD features must serve travel planning. Avoid unrelated admin features.

### Core CRUD Modules

- User profile and travel preference.
- City resource.
- Attraction resource.
- Hotel resource.
- Restaurant resource.
- Travel tags.
- Favorite attraction.
- Favorite trip.
- User travel note.
- Trip history.
- AI analysis record.

### TODO

- [x] Design MySQL tables:
  - `tm_user`.
  - `tm_user_preference`.
  - `tm_city`.
  - `tm_attraction`.
  - `tm_hotel`.
  - `tm_restaurant`.
  - `tm_travel_tag`.
  - `tm_favorite`.
  - `tm_travel_note`.
  - `tm_trip_plan`.
  - `tm_trip_day`.
  - `tm_trip_item`.
  - `tm_ai_analysis_record`.
- [x] Add seed data:
  - sample cities.
  - sample attractions.
  - sample hotels.
  - sample restaurants.
  - sample travel tags.
- [x] Implement backend CRUD APIs:
  - paged list.
  - detail.
  - create.
  - update.
  - delete.
  - status enable/disable where useful.
- [x] Implement query filters:
  - keyword.
  - city.
  - category.
  - tag.
  - rating range.
- [x] Implement trip history persistence.
- [x] Replace empty trip history response with real MySQL query.
- [x] Implement favorite APIs:
  - add favorite.
  - cancel favorite.
  - list favorites.
- [x] Implement travel note APIs:
  - create note.
  - edit note.
  - delete note.
  - list by user or attraction.
- [x] Implement AI analysis record APIs:
  - list records.
  - detail.
  - delete record.
- [x] Build Vue pages:
  - user profile.
  - city list.
  - attraction list.
  - hotel list.
  - restaurant list.
  - trip history.
  - favorites.
  - travel notes.
  - AI records.

### Done Criteria

- [x] CRUD APIs support normal create/read/update/delete flows.
- [x] CRUD pages can call backend APIs.
- [x] Trip history is persisted in MySQL.
- [x] AI records can be stored and queried.
- [x] SQL scripts can initialize a demo database.

### Out Of Scope

- Do not add unrelated ERP-style management pages.
- Do not make Python AI depend on database directly.
- Do not place frontend business logic inside backend templates.

## Phase 3: Intelligent Travel Planning Core

Goal: implement the main product value: generate, review, store, and display personalized travel plans.

Business flow:

```text
User request
-> map and POI context
-> travel content context
-> large model trip planning
-> trip review
-> result persistence
-> frontend display
```

Wang-ke-li owns this phase as the technical lead.

### TODO

- [x] Implement travel planning request model:
  - destination city list.
  - start date.
  - end date.
  - travel days.
  - transportation.
  - accommodation.
  - budget.
  - preferences.
  - free text requirement.
  - output language.
- [x] Implement async planning task workflow:
  - submit task.
  - task status.
  - task progress.
  - task failure reason.
  - task result.
- [x] Implement progress delivery:
  - WebSocket or polling.
  - frontend progress display.
- [x] Integrate map context:
  - geocode.
  - attraction POI.
  - hotel POI.
  - restaurant POI.
  - weather forecast.
- [x] Integrate travel content context:
  - travel note extraction.
  - content candidates.
  - recommendation reasons.
  - warning tips.
- [x] Implement large model agents:
  - research agent.
  - planner agent.
  - review agent.
  - chat assistant agent.
- [x] Enforce structured output:
  - JSON-only model result.
  - DTO conversion.
  - validation.
  - graceful failure message.
- [x] Generate trip plan:
  - daily schedule.
  - attractions.
  - meals.
  - hotel.
  - transportation.
  - weather tips.
  - budget.
- [x] Store generated plan in MySQL.
- [x] Implement trip detail API.
- [x] Implement trip copy API.
- [x] Implement trip delete API.
- [x] Implement trip chat API based on saved trip plan.
- [x] Build Vue pages:
  - planning form.
  - progress view.
  - trip result.
  - trip detail.
  - trip history.
  - trip chat.

### Done Criteria

- [x] A user can submit a travel planning request.
- [x] The backend can show planning progress.
- [x] The large model returns a structured trip plan.
- [x] The review step validates the result.
- [x] The result is saved in MySQL.
- [x] The frontend can display the trip plan.
- [x] The user can open historical trip plans.

### Out Of Scope

- Do not require a self-hosted large model.
- Do not let the model fabricate tool data when external data fails.
- Do not block the HTTP request until all planning work is complete.

## Phase 4: Local Python AI And Java-Python REST Integration

Goal: add local AI capabilities that strengthen travel planning rather than becoming standalone demos.

Python service owner: Zhu Qicheng.

Java integration owner: Wang-ke-li.

### Python AI Capability 1: Travel Image Detection

Purpose:

- Analyze attraction, restaurant, hotel, and travel scene images.
- Produce tags and descriptions that can improve POI profiles and travel suggestions.

Endpoint:

```text
POST /api/vision/detect
```

Input:

- image file or image URL.
- optional city.
- optional resource type.

Output:

- detected labels.
- confidence scores.
- scene tags.
- generated image summary.
- risk hints where possible.

TODO:

- [x] Add image upload handling.
- [x] Integrate YOLO-compatible optional model boundary with rule fallback.
- [x] Normalize detection labels into travel tags.
- [x] Return stable JSON.
- [x] Add sample images for tests.
- [x] Add Java API to call Python detection.
- [x] Store detection result in `tm_ai_analysis_record`.
- [x] Show detection result in Vue.

### Python AI Capability 2: Trip Comfort Scoring

Purpose:

- Evaluate whether a generated trip is too dense, too risky, or unsuitable for the user profile.

Endpoint:

```text
POST /api/trip/evaluate
```

Input:

- trip days.
- attractions per day.
- transportation.
- city transfers.
- weather.
- user preferences.
- budget.

Output:

- comfort score.
- risk level.
- daily risk items.
- optimization suggestions.

TODO:

- [x] Define scoring features.
- [x] Implement initial rule-based scoring.
- [x] Keep optional machine learning model disabled because no training data is documented.
- [x] Return daily and overall scores.
- [x] Add Java API to call trip evaluation.
- [x] Store evaluation result.
- [x] Include score in trip review report.
- [x] Show score in Vue trip detail page.

### Python AI Capability 3: Travel Text Sentiment And Keyword Analysis

Purpose:

- Analyze travel notes, comments, and content snippets.
- Extract recommendation points and warning points.

Endpoint:

```text
POST /api/content/analyze
```

Input:

- text.
- city.
- attraction name.
- language.

Output:

- sentiment.
- keywords.
- positive highlights.
- negative warnings.
- suitable traveler types.

TODO:

- [x] Implement text preprocessing.
- [x] Implement keyword extraction.
- [x] Implement sentiment scoring.
- [x] Return stable JSON.
- [x] Add Java API to call text analysis.
- [x] Store analysis result.
- [x] Use analysis result in travel content context.
- [x] Show analysis result in travel note page.

### Java-Python Integration TODO

- [x] Add Python service base URL config.
- [x] Add Java REST client.
- [x] Add timeout and failure handling.
- [x] Add DTOs for all Python endpoints.
- [x] Add controller endpoints for frontend.
- [x] Add AI analysis record persistence.
- [x] Add integration tests with mock Python responses.
- [x] Add API document:
  - `docs/api/java-python-api.md`.

### Done Criteria

- [x] Java backend can call all three Python endpoints.
- [x] Python AI service returns stable JSON.
- [x] AI results are stored in MySQL.
- [x] AI results are visible in frontend.
- [x] Trip comfort scoring is connected to trip review.
- [x] Text analysis is connected to travel notes or content context.
- [x] Image detection is connected to travel resource profiles or AI records.

### Out Of Scope

- Do not make Python service access MySQL directly.
- Do not require GPU for the demo path.
- Do not block trip planning if Python AI is temporarily unavailable.
- Do not train a model unless the dataset is documented.

## Phase 5: Integration, Testing, Agile Materials, And Delivery

Goal: complete the full delivery package and show a clear agile team workflow.

Wang-ke-li leads final integration, merge review, release checklist, and risk handling.

Zhang Shuai leads test reports and document assembly.

### Integration TODO

- [ ] Run backend.
- [ ] Run frontend.
- [ ] Run Python AI service.
- [ ] Run MySQL.
- [ ] Verify frontend login or profile flow.
- [ ] Verify travel resource CRUD.
- [ ] Verify trip planning.
- [ ] Verify trip history.
- [ ] Verify image detection.
- [ ] Verify trip comfort scoring.
- [ ] Verify text analysis.
- [ ] Verify Java-Python failure fallback.
- [ ] Verify large model API failure fallback.
- [ ] Verify deployment instructions.

### Test TODO

- [ ] Unit tests for backend services.
- [ ] Controller tests for key APIs.
- [ ] Python API tests.
- [ ] Frontend smoke tests.
- [ ] CRUD test cases.
- [ ] Trip planning test cases.
- [ ] Java-Python integration test cases.
- [ ] Failure case test cases.
- [ ] Create final test report.

### Agile Materials TODO

Create the following documents:

- [ ] `docs/agile/project-kickoff-meeting.md`.
- [ ] `docs/agile/team-roles.md`.
- [ ] `docs/agile/product-backlog.md`.
- [ ] `docs/agile/sprint-1-plan.md`.
- [ ] `docs/agile/sprint-1-daily-standups.md`.
- [ ] `docs/agile/sprint-1-review-retrospective.md`.
- [ ] `docs/agile/sprint-2-plan.md`.
- [ ] `docs/agile/sprint-2-daily-standups.md`.
- [ ] `docs/agile/sprint-2-review-retrospective.md`.
- [ ] `docs/agile/sprint-3-plan.md`.
- [ ] `docs/agile/sprint-3-daily-standups.md`.
- [ ] `docs/agile/sprint-3-review-retrospective.md`.
- [ ] `docs/agile/defect-fix-log.md`.
- [ ] `docs/agile/final-project-summary.md`.

### Technical Documents TODO

- [ ] `docs/design/requirements-specification.md`.
- [ ] `docs/design/system-architecture.md`.
- [ ] `docs/design/database-design.md`.
- [ ] `docs/design/module-design.md`.
- [ ] `docs/api/backend-api.md`.
- [ ] `docs/api/java-python-api.md`.
- [ ] `docs/test/test-plan.md`.
- [ ] `docs/test/test-cases.md`.
- [ ] `docs/test/test-report.md`.
- [ ] `docs/deploy/deployment-guide.md`.

### Git Delivery TODO

- [ ] Ensure all commits have one of the four approved authors.
- [ ] Ensure each feature branch has meaningful commits.
- [ ] Ensure Wang-ke-li has visible leadership commits:
  - project scope.
  - sprint planning.
  - architecture.
  - backend core.
  - AI orchestration.
  - Java-Python integration.
  - final integration.
- [ ] Merge feature branches into `develop`.
- [ ] Merge `develop` into `main`.
- [ ] Tag final version:
  - `v1.0.0`.
- [ ] Add release notes.

### Done Criteria

- [ ] The full system can run end to end.
- [ ] Frontend, backend, Python AI service, and MySQL are connected.
- [ ] Traditional CRUD features are complete.
- [ ] Local Python AI features are complete.
- [ ] Large model API integration is complete.
- [ ] Agile materials are complete.
- [ ] Git history shows four-person collaboration.
- [ ] Final README explains how to run the project.

### Out Of Scope

- Do not include fake attendance signatures.
- Do not include fake photos.
- Do not claim private real-world events that cannot be verified by the team.

## Final Acceptance Checklist

- [ ] JDK version is 17 or lower.
- [ ] Python version is 3.10 or 3.12.
- [ ] MySQL version is 8.0 or 5.7.
- [ ] Spring Boot backend runs.
- [ ] Vue frontend runs.
- [ ] Python AI service runs.
- [ ] Java calls Python through REST.
- [ ] The system includes large model API integration.
- [ ] The system includes at least one deep learning or machine learning feature.
- [ ] The system includes enough traditional CRUD features.
- [ ] The system has complete agile process documents.
- [ ] Git history shows Wang-ke-li, Chen Wenzhe, Zhu Qicheng, and Zhang Shuai.
- [ ] Wang-ke-li is clearly visible as team lead and core technical contributor.
