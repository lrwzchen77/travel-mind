# Test Cases

## Backend

| ID | Area | Steps | Expected Result |
| --- | --- | --- | --- |
| BE-01 | Health | Call `GET /health`. | HTTP 200 with `status=healthy`. |
| BE-02 | CRUD list | Call `GET /api/cities?pageSize=5`. | Paged `PageResult` with `records`. |
| BE-03 | CRUD lifecycle | Create, detail, update, and delete a city through `/api/cities`. | All calls return success; deleted city is no longer listed. |
| BE-04 | Profile | Call `GET /api/users/profile?userId=1001`, then `PUT` profile fields. | User and preference payloads are returned. |
| BE-05 | Trip planning | Submit `/api/trip/plan` and poll `/api/trip/status/{taskId}`. | Task reaches `completed` and returns a saved `plan_id`. |
| BE-06 | Trip history | Call `GET /api/trip/history?limit=3`. | Recently generated plans are returned. |

## Python AI And Java-Python Integration

| ID | Area | Steps | Expected Result |
| --- | --- | --- | --- |
| AI-01 | Python health | Call `GET /health` on port `19080`. | Envelope has `status=healthy`. |
| AI-02 | Vision | Call Java `POST /api/ai/vision/detect`. | `success=true`, labels and scene tags are returned and persisted. |
| AI-03 | Trip scoring | Call Java `POST /api/ai/trip/evaluate`. | Comfort score, risk level, daily risks, and suggestions are returned. |
| AI-04 | Text analysis | Call Java `POST /api/ai/content/analyze`. | Sentiment, keywords, highlights, warnings, and traveler types are returned. |
| AI-05 | Saved comfort | Call `GET /api/ai/trip/{planId}/comfort`. | Latest trip scoring record is returned from MySQL. |
| AI-06 | Python unavailable | Stop FastAPI and call Java AI endpoint. | HTTP request succeeds; `data.success=false`; failed record is stored. |

## Frontend

| ID | Area | Steps | Expected Result |
| --- | --- | --- | --- |
| FE-01 | Route smoke | Open `/`, `/planning`, `/trip-history`, `/ai-lab`, `/ai-records`, `/trip/{id}`. | Vite serves the SPA with HTTP 200. |
| FE-02 | AI Lab | Use image, trip, and text forms. | Results render from Java `/api/ai/*` endpoints. |
| FE-03 | Trip detail | Open a saved trip detail page. | Trip plan and comfort panel are visible. |
| FE-04 | Resource pages | Open CRUD pages for cities, attractions, hotels, restaurants, notes, favorites, and AI records. | Lists load from backend resource APIs. |

## Failure Handling

| ID | Area | Steps | Expected Result |
| --- | --- | --- | --- |
| FH-01 | Large model fallback | Run without runtime AI, map, and XHS settings, then submit trip planning. | Backend uses MySQL Demo Planner and task completes. |
| FH-02 | Python scoring fallback | Stop Python AI and submit trip planning. | Trip still completes; comfort scoring failure is logged and stored. |
| FH-03 | External data fallback | Disable map/content providers. | Planning reports local resource fallback instead of blocking HTTP request. |
