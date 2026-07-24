# Backend API

Base URL: `http://localhost:8080`. Sa-Token uses the `Authorization` header. Public endpoints do not require it; user and admin namespaces require their matching role.

## Authentication

- `POST /api/user/auth/login`: consumer account login.
- `GET /api/user/auth/me`: current consumer session.
- `POST /api/user/auth/logout`: consumer logout.
- `POST /api/admin/auth/login`: administrator login.
- `GET /api/admin/auth/me`: current administrator session.
- `POST /api/admin/auth/logout`: administrator logout.

Login body:

```json
{"username": "demo_user", "password": "configured-password"}
```

## Public Discovery

- `GET /api/public/resources/{resourceKey}`: paged active resources.
- `GET /api/public/resources/{resourceKey}/{id}`: active resource detail.

Public resource keys are `cities`, `attractions`, `hotels`, `restaurants`, and `travel-tags`. Writes and inactive content are never exposed here.

## Consumer APIs

- `GET /api/user/profile`: current user's profile and preferences.
- `PUT /api/user/profile`: update the current user's profile and preferences.
- `GET /api/user/library/{resourceKey}`: current user's favorites, notes, or AI records.
- `POST|PUT|DELETE /api/user/library/{resourceKey}`: current user's favorites and notes only.
- `POST /api/user/trip/plan`: submit an asynchronous trip task.
- `GET /api/user/trip/status/{taskId}`: current user's task progress.
- `GET /api/user/trip/history?limit=8`: current user's saved trips.
- `GET /api/user/trip/{id}`: owned trip detail.
- `POST /api/user/trip/{id}/copy`: copy an owned trip.
- `DELETE /api/user/trip/{id}`: delete an owned trip.
- `POST /api/user/trip/{id}/chat`: chat about an owned trip.
- `WS /api/user/trip/ws/{taskId}`: authenticated progress stream; browser clients pass the same token as the `Authorization` query parameter.
- `POST /api/user/ai/vision/detect`
- `POST /api/user/ai/trip/evaluate`
- `POST /api/user/ai/content/analyze`
- `GET /api/user/ai/trip/{id}/comfort`
- `GET /api/user/ai/trip/{id}/comfort/feedback`
- `POST /api/user/ai/trip/{id}/comfort/feedback`
- `GET /api/admin/ai/travel-comfort/feedback/stats`

User IDs are taken only from the authenticated session. Client-supplied `userId` values are not accepted.

## Admin APIs

- `GET|POST /api/admin/resources/{resourceKey}`
- `GET|PUT|DELETE /api/admin/resources/{resourceKey}/{id}`
- `PUT /api/admin/resources/{resourceKey}/{id}/status?status=1`
- `GET /api/admin/settings`: redacted runtime configuration status.
- `PUT /api/admin/settings`: update runtime configuration in the Java process.
- `POST /api/admin/ai/**`: administrator AI validation endpoints matching the user AI contract.

Supported admin resource keys include users, preferences, cities, attractions, hotels, restaurants, tags, favorites, notes, trip plans, and AI records.

## Status Rules

- `200`: request succeeded.
- `400`: validation, login credential, or business error.
- `401`: no valid session.
- `403`: valid session with the wrong portal role.
- `404`: resource does not exist or is not owned by the current user.

See `docs/api/java-python-api.md` for the internal Java-to-Python contract.
