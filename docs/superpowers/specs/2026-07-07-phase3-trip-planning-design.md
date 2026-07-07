# Phase 3 Trip Planning Design

## Goal

Phase 3 makes Travel Mind's core travel planning flow usable end to end: submit a planning request, track progress, generate a structured trip plan, review it, persist it, reopen it from history, copy/delete it, and chat about a saved plan.

## Chosen Approach

Use the existing real Agent pipeline when runtime configuration is complete. The pipeline remains:

```text
request -> map/content research -> planner agent -> review agent -> structured DTO -> persistence -> frontend
```

When AI, map, or content keys are missing, the backend must not fabricate external tool data. Instead it uses a deterministic MySQL demo planner based on Phase 2 tables: cities, attractions, hotels, restaurants, travel notes, and trip history. This keeps the local Docker demo runnable while preserving the real Agent path for configured environments.

## Backend Design

`TripTaskService` remains the async orchestration entry point. It validates the request, starts a background task, updates polling/WebSocket progress, calls a planner, persists the result, and exposes the saved plan id in the completed result.

Planning is split behind a focused interface:

- Real planner: existing `TripResearchService` + `TripAiPlannerService`.
- Demo planner: reads MySQL travel resources and generates `TripPlanResponse`.
- Review: both paths must pass a deterministic structure validator that checks city, dates, day count, daily attractions/meals, hotel, transportation, weather tips, and budget.

Persistence writes:

- `tm_trip_plan.raw_plan_json` for the full structured response.
- `tm_trip_day` for day summaries.
- `tm_trip_item` for attractions, meals, hotels, transportation, and tips.

New trip APIs:

- `POST /api/trip/plan`
- `GET /api/trip/status/{taskId}`
- `GET /api/trip/{id}`
- `POST /api/trip/{id}/copy`
- `DELETE /api/trip/{id}`
- `POST /api/trip/{id}/chat`
- Existing `/api/trip/history` returns saved plans.

## Frontend Design

Vue adds a complete planning workflow:

- Planning form for city list, dates, days, transportation, accommodation, budget, preferences, language, and free text.
- Progress area using polling first, with WebSocket-compatible payloads preserved.
- Result view showing daily schedule, attractions, meals, hotel, weather tips, budget, and graph summary.
- Trip history rows open the saved detail page.
- Trip detail page supports copy, delete, and chat.

The UI remains operational-tool oriented: compact forms, clear tables, and direct controls rather than a marketing page.

## Error Handling

Missing external runtime configuration triggers demo planner mode, not a hard failure. Real Agent failures return a graceful task failure unless the demo planner can produce a valid plan. Invalid planner output fails review with a readable reason. Delete uses soft delete through existing table conventions.

## Testing And Acceptance

Backend tests cover demo planner generation, plan review, persistence detail/copy/delete/chat, and task completion payloads. Frontend tests cover API client contracts and route/menu availability. Full verification requires Maven tests, frontend tests/build, SQL presence, backend smoke, and Vite route smoke.

Phase 3 is complete only when the todo checklist has no unchecked Phase 3 items and a local Docker MySQL run can submit a plan, observe progress, persist it, open it from history, copy/delete it, and chat about it.
