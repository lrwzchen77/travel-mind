# Phase 4 Python AI Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add runnable local Python AI capabilities and connect them to Java, MySQL, trip review, and Vue.

**Architecture:** FastAPI owns deterministic local AI endpoints and never touches the database. Spring Boot calls Python through a small Java 17 HTTP client, persists analysis records, and exposes frontend-facing APIs. Vue adds an AI Lab plus trip detail comfort score display.

**Tech Stack:** Python 3.10/3.12, FastAPI, pytest, Java 17, Spring Boot, NamedParameterJdbcTemplate, Vue 3, Vite, Vitest.

---

### Task 1: Python AI Endpoints

**Files:**
- Modify: `python-ai/app/main.py`
- Create: `python-ai/tests/test_phase4_ai.py`

- [x] Write tests for `/api/vision/detect`, `/api/trip/evaluate`, and `/api/content/analyze`.
- [x] Verify tests fail because endpoints do not exist.
- [x] Implement Pydantic DTOs and deterministic services in `main.py`.
- [x] Verify Python tests pass with `.venv/Scripts/python.exe -m pytest`.

### Task 2: Java Python Client And DTOs

**Files:**
- Create: `modules/trip/src/main/java/com/zkry/trip/dto/ai/*.java`
- Create: `modules/trip/src/main/java/com/zkry/trip/service/PythonAiClient.java`
- Create: `modules/trip/src/test/java/com/zkry/trip/service/PythonAiClientTest.java`
- Modify: `app/src/main/resources/application.yml`

- [x] Write tests for URL construction, JSON parsing, and unavailable-service fallback.
- [x] Verify tests fail because the client does not exist.
- [x] Implement Java 17 `HttpClient` based client with configurable base URL and timeout.
- [x] Verify trip module tests pass.

### Task 3: Analysis Record Persistence And APIs

**Files:**
- Create: `modules/trip/src/main/java/com/zkry/trip/service/AiAnalysisRecordService.java`
- Create: `modules/trip/src/main/java/com/zkry/trip/service/TravelAiApplicationService.java`
- Create: `app/src/main/java/com/zkry/api/trip/TravelAiController.java`
- Create: `modules/trip/src/test/java/com/zkry/trip/service/TravelAiApplicationServiceTest.java`

- [x] Write tests proving each AI call is persisted to `tm_ai_analysis_record`.
- [x] Verify tests fail because services do not exist.
- [x] Implement persistence and controller-facing service methods.
- [x] Verify Java tests pass.

### Task 4: Trip Review Comfort Integration

**Files:**
- Modify: `modules/trip/src/main/java/com/zkry/trip/service/TripTaskService.java`
- Modify: `app/src/main/java/com/zkry/api/trip/TripController.java`
- Test: existing `modules/trip/src/test/java/com/zkry/trip/service/*`

- [x] Add test coverage for comfort scoring fallback not blocking plan completion.
- [x] Call Python trip evaluation after saving a generated trip.
- [x] Add `GET /api/trip/{id}/comfort` for saved trip comfort data.
- [x] Verify backend tests pass.

### Task 5: Vue AI Lab And Trip Detail Display

**Files:**
- Create: `frontend/src/api/ai.js`
- Create: `frontend/src/api/ai.test.js`
- Create: `frontend/src/views/AiLabView.vue`
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/layout/menu.js`
- Modify: `frontend/src/views/TripDetailView.vue`
- Test: `frontend/src/app-shell.test.js`

- [x] Write Vitest tests for AI API wrapper and route/menu registration.
- [x] Verify tests fail for missing wrapper/route.
- [x] Implement AI Lab forms and trip detail comfort score panel.
- [x] Verify `npm test` and `npm run build`.

### Task 6: Documentation, TODO Updates, And Smoke

**Files:**
- Create: `docs/api/java-python-api.md`
- Modify: `docs/project-todo-workflow.md`
- Modify: `.env.example`
- Modify: `README.md`

- [x] Document request/response examples for all Java-Python APIs.
- [x] Mark Phase 4 checklist items complete only after verification.
- [x] Run Python service, Java backend, MySQL/Redis, and frontend route smoke.
- [x] Run final verification: Python tests, Maven tests, app package, frontend tests/build.
