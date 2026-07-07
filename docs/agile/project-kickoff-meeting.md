# Project Kickoff Meeting

## Purpose

Align the team on Travel Mind: a full-stack intelligent travel planning system with CRUD management, large-model trip planning, and local Python AI capabilities.

## Decisions

- Use Java 17, Spring Boot 3, Vue 3 + Vite, Python FastAPI, MySQL, Redis, and REST integration.
- Keep frontend, Java backend, Python AI, and database as separately runnable services.
- Make Java the orchestration and persistence owner; Python AI must not access MySQL directly.
- Use deterministic local fallback paths so demos work without paid external credentials.

## Initial Risks

- External map/content/large-model credentials may be unavailable during demo.
- Java-Python API contracts need stable envelopes and timeout handling.
- Git history and agile documents must be kept consistent with team roles.

## Immediate Actions

- Wang-ke-li: architecture, backend core, AI orchestration.
- Chen Wenzhe: Vue application shell and user workflows.
- Zhu Qicheng: FastAPI AI endpoints and tests.
- Zhang Shuai: SQL schema, CRUD validation, test and delivery documents.
