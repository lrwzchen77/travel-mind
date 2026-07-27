# Backend API

Base URL: `http://localhost:8080`. Sa-Token uses the `Authorization` header. Public endpoints do not require it; user and admin namespaces require their matching role.

## Authentication

- `POST /api/user/auth/login`: consumer account login.
- `POST /api/user/auth/register`: create a consumer account and return its login session.
- `GET /api/user/auth/me`: current consumer session.
- `POST /api/user/auth/logout`: consumer logout.
- `POST /api/admin/auth/login`: administrator login.
- `GET /api/admin/auth/me`: current administrator session.
- `POST /api/admin/auth/logout`: administrator logout.
- `PUT /api/user/account/password`: change password and invalidate existing tokens.
- `GET /api/user/account/export`: export the current user's data.
- `DELETE /api/user/account`: deactivate the current account.

Login body:

```json
{"username": "demo_user", "password": "configured-password"}
```

Registration body:

```json
{"username": "traveler", "nickname": "旅行者", "password": "at-least-10-characters"}
```

## Public Discovery

- `GET /api/public/resources/{resourceKey}`: paged active resources.
- `GET /api/public/resources/{resourceKey}/{id}`: active resource detail.

Public resource keys are `cities`, `attractions`, `hotels`, `restaurants`, and `travel-tags`. Writes and inactive content are never exposed here.

## Consumer APIs

- `GET /api/user/profile`: current user's profile and preferences.
- `PUT /api/user/profile`: update the current user's profile and preferences.
- `GET /api/user/library/{resourceKey}`: current user's favorites, notes, or AI records.
- `POST /api/user/library/{resourceKey}`: create a favorite or private note.
- `PUT /api/user/library/travel-notes/{id}`: edit an owned note.
- `DELETE /api/user/library/{resourceKey}/{id}`: delete an owned favorite or note.
- `POST /api/user/trip/plan`: submit an asynchronous trip task.
- `GET /api/user/trip/status/{taskId}`: current user's task progress.
- `GET /api/user/trip/history?limit=8`: current user's saved trips.
- `GET /api/user/trip/{id}`: owned trip detail.
- `PUT /api/user/trip/{id}`: edit an owned saved trip.
- `POST /api/user/trip/{id}/copy`: copy an owned trip.
- `DELETE /api/user/trip/{id}`: delete an owned trip.
- `POST /api/user/trip/{id}/chat`: chat about an owned trip.
- `POST /api/user/trip/{id}/journal`: create or reopen the trip's journal.
- `POST /api/user/trip/tasks/{taskId}/cancel`: cancel an active planning task.
- `POST /api/user/trip/tasks/{taskId}/retry`: retry a failed or cancelled planning task.
- `GET|PUT|DELETE /api/user/assistant/conversations/{id}`: read, rename, or delete an AI conversation.
- `POST /api/user/assistant/conversations/{id}/stop`: stop active streaming generation.
- `POST /api/user/assistant/ask/stream`: SSE assistant response with model/fallback metadata.
- `GET /api/user/notifications`: recent account notifications.
- `POST /api/user/notifications/{id}/read` and `/read-all`: mark notifications read.
- `GET|POST /api/user/journals`: list or create owned travel journals.
- `GET|PUT|DELETE /api/user/journals/{id}`: read, edit, or delete an owned journal.
- `POST /api/user/journals/{id}/publish`: publish an owned journal.
- `POST /api/user/journals/{id}/photos` and `DELETE /api/user/journals/{id}/photos/{photoId}`: add or remove photos.
- `POST /api/user/journals/{id}/locations` and `DELETE /api/user/journals/{id}/locations/{locationId}`: add or remove locations.
- `GET /api/user/recommendations`: personalized city or POI recommendations.
- `POST /api/user/recommendations/{id}/feedback`: record `click`, `ignore`, `save`, or `like` feedback.
- `PUT /api/user/inspirations/posts/{id}`: edit a post; public posts return to pending review.
- `POST /api/user/inspirations/posts/{id}/submit`: submit a private or rejected post for review.
- `POST /api/user/ai/vision/detect`
- `POST /api/user/ai/content/analyze`
- `GET /api/user/ai/trip/{id}/comfort`
- `GET /api/user/ai/trip/{id}/comfort/feedback`
- `POST /api/user/ai/trip/{id}/comfort/feedback`
- `GET /api/admin/ai/travel-comfort/feedback/stats`

User IDs are taken only from the authenticated session. Client-supplied `userId` values are not accepted.

## Admin APIs

- `GET|POST /api/admin/resources/{resourceKey}`
- `PUT|DELETE /api/admin/resources/{resourceKey}/{id}`
- `PUT /api/admin/resources/{resourceKey}/{id}/status?status=1`
- `PUT /api/admin/users/{id}/password`: reset a password and invalidate existing tokens.
- `PUT /api/admin/users/{id}/role`: set `user` or `admin` and invalidate existing tokens.
- `POST /api/admin/inspirations/{id}/review`: approve (`status=1`) or reject (`status=2`, reason required) community content.
- `GET /api/admin/settings`: redacted runtime configuration status.
- `PUT /api/admin/settings`: update runtime configuration in the Java process.
- `POST /api/admin/ai/vision/detect`, `/trip/evaluate`, and `/content/analyze`: administrator AI validation endpoints.
- `POST /api/admin/recommendations/reindex`: rebuild the destination vector index.

Supported admin resource keys include users, preferences, cities, attractions, hotels, restaurants, tags, notes, trip plans, and AI records. Personal favorites stay inside the user library boundary.

## Status Rules

- `200`: request succeeded.
- `400`: validation, login credential, or business error.
- `401`: no valid session.
- `403`: valid session with the wrong portal role.
- `404`: resource does not exist or is not owned by the current user.

See `docs/api/java-python-api.md` for the internal Java-to-Python contract.
