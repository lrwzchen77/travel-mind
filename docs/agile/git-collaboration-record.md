# Git Collaboration Record

## Purpose

This record supports the Phase 5 Git delivery requirement in
`docs/project-todo-workflow.md`. It documents the expected branch model,
approved author identities, and collaboration evidence for the four-person
Travel Mind delivery.

## Approved Authors

```text
Wang-ke-li <Wang-ke-li@users.noreply.gitee.com>
Chen Wenzhe <chen-wenzhe@example.local>
Zhu Qicheng <zhu-qicheng@example.local>
Zhang Shuai <zhang-shuai@example.local>
```

## Branch Model

```text
main
develop
feature/backend-core
feature/frontend-vue
feature/python-ai
feature/database-crud
feature/docs-test
```

## Verification Commands

```powershell
git shortlog -sne --all
git log --all --decorate --graph --oneline
git for-each-ref --format="%(refname:short) %(objectname:short) %(subject)" refs/heads refs/tags
```

## Frontend Collaboration Entries

### Vue Shell

Chen Wenzhe owns the Vue 3 + Vite application shell, including routing,
layout structure, API client conventions, and environment configuration.
This matches the Phase 1 frontend initialization requirement.

### Resource Pages

Chen Wenzhe implements travel resource pages for city, attraction, hotel,
restaurant, favorites, notes, history, and AI records. These screens call the
Spring Boot CRUD APIs and keep frontend business logic inside Vue components
and service modules.

### Planning Flow

Chen Wenzhe connects the trip planning form, task progress polling, trip
result view, trip detail, copy, delete, and chat interactions. The UI presents
structured itinerary data returned by the Java backend.

### AI Result Views

Chen Wenzhe adds the AI Lab and result panels for image detection, trip comfort
scoring, and travel text analysis. These views expose Java-Python REST results
without making the frontend call the Python service directly.

### Frontend Validation

Chen Wenzhe maintains Vitest smoke coverage for core user flows and confirms
the Vite production build. Frontend verification is recorded in the final test
report and release notes.

### Frontend Delivery Notes

Chen Wenzhe documents UI delivery boundaries: Vue owns interaction and result
presentation, while Spring Boot owns orchestration and persistence. This keeps
the frontend/backend separation required by the locked stack.
