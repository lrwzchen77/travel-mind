# Phase 5 Integration Delivery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the final integration, testing, agile materials, technical documents, and release checklist for Travel Mind.

**Architecture:** Treat Phase 5 as delivery hardening rather than new product work. Verify the already-built Vue, Spring Boot, FastAPI, MySQL, and Redis system end to end, then write concise evidence-backed documents and update the master TODO only after checks are complete.

**Tech Stack:** Java 17, Spring Boot 3, Maven, Vue 3, Vite, Vitest, Python FastAPI, pytest, MySQL 8, Redis 7, Docker.

---

### Task 1: Integration Smoke

**Files:**
- Read: `docs/project-todo-workflow.md`
- Read: `README.md`
- Test: running services on ports `8080`, `5173`, `19080`, `3306`, `6379`

- [x] Confirm backend, frontend, Python AI, MySQL, and Redis are listening.
- [x] Call backend health, Python health, frontend SPA routes, CRUD list/detail/create/update/delete flow, profile flow, trip planning flow, trip history, image detection, trip comfort scoring, text analysis, Java-Python failure fallback, and large-model fallback.
- [x] Record the exact commands and observed results for the final test report.

### Task 2: Test Evidence Documents

**Files:**
- Modify: `docs/test/test-plan.md`
- Create: `docs/test/test-cases.md`
- Create: `docs/test/test-report.md`

- [x] Replace the Phase 1-only test plan with full-system Phase 5 scope.
- [x] Write test cases covering backend unit/controller tests, Python APIs, frontend smoke, CRUD, trip planning, Java-Python integration, and failure handling.
- [x] Write the final report with the latest command evidence and smoke results.

### Task 3: Agile Materials

**Files:**
- Create: all required `docs/agile/*.md` Phase 5 files.

- [x] Write project kickoff, roles, backlog, sprint plans, daily standups, review/retrospectives, defect log, and final summary.
- [x] Keep material plausible and process-focused without claiming unverifiable attendance signatures or photos.

### Task 4: Technical Documents

**Files:**
- Create or modify: `docs/design/requirements-specification.md`, `docs/design/system-architecture.md`, `docs/design/database-design.md`, `docs/design/module-design.md`, `docs/api/backend-api.md`, `docs/deploy/deployment-guide.md`

- [x] Document requirements, architecture, database tables, module boundaries, backend APIs, Java-Python APIs, and deployment steps.
- [x] Ensure documents match the locked stack and implemented endpoints.

### Task 5: Release Checklist And TODO Updates

**Files:**
- Modify: `docs/project-todo-workflow.md`
- Create: `docs/deploy/release-notes-v1.0.0.md`

- [x] Update Phase 5 Integration, Test, Agile, Technical Documents, and Done Criteria checkboxes based on completed work.
- [x] Update Final Acceptance Checklist items that are verified by evidence.
- [x] Add release notes for `v1.0.0`.

### Task 6: Final Verification

**Files:**
- Test: `python-ai`, Maven root, `frontend`

- [x] Run `python-ai/.venv/Scripts/python.exe -m pytest -q`.
- [x] Run `mvn test`.
- [x] Run `mvn -pl app -am package -DskipTests`.
- [x] Run `cd frontend && npm test`.
- [x] Run `cd frontend && npm run build`.
- [x] Recheck Phase 5 unchecked count and report any Git Delivery items that require explicit commit/merge/tag approval.
