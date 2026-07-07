# Backend API

Base URL: `http://localhost:8080`.

## Health And Settings

- `GET /health`: backend health.
- `GET /api/settings`: runtime settings summary.
- `PUT /api/settings`: update runtime settings.

## User Profile

- `GET /api/users/profile?userId=1001`: get user and latest preference.
- `PUT /api/users/profile?userId=1001`: update user and preference.

Example:

```json
{
  "user": {"nickname": "Demo User"},
  "preference": {"travel_style": "relaxed", "preferred_city": "Hangzhou"}
}
```

## Generic CRUD Resources

Pattern:

- `GET /api/{resourceKey}`: paged list.
- `GET /api/{resourceKey}/{id}`: detail.
- `POST /api/{resourceKey}`: create.
- `PUT /api/{resourceKey}/{id}`: update.
- `PUT /api/{resourceKey}/{id}/status?status=1`: update status where supported.
- `DELETE /api/{resourceKey}/{id}`: logical delete.

Supported `resourceKey` values include `users`, `user-preferences`, `cities`, `attractions`, `hotels`, `restaurants`, `travel-tags`, `favorites`, `travel-notes`, `trip-plans`, and `ai-records`.

Common filters include `keyword`, `cityId`, `category`, `tag`, `ratingMin`, `ratingMax`, `userId`, `targetType`, `targetId`, `analysisType`, `status`, `pageNum`, and `pageSize` when supported by the resource.

## Trip Planning

- `POST /api/trip/plan`: submit async trip planning task.
- `GET /api/trip/status/{taskId}`: poll task progress/result.
- `GET /api/trip/history?limit=8`: list recent saved trips.
- `GET /api/trip/{id}`: trip detail.
- `POST /api/trip/{id}/copy?userId=1001`: copy saved trip.
- `DELETE /api/trip/{id}`: delete saved trip.
- `POST /api/trip/{id}/chat`: chat about a saved trip.

Trip request example:

```json
{
  "city": "Hangzhou",
  "start_date": "2026-08-01",
  "end_date": "2026-08-02",
  "travel_days": 2,
  "transportation": "公共交通",
  "accommodation": "舒适型酒店",
  "budget": "3000",
  "preferences": ["湖景", "美食", "轻松"],
  "free_text_input": "节奏轻松，适合第一次到杭州。",
  "language": "zh"
}
```

## AI Endpoints

- `POST /api/ai/vision/detect`: image detection through Python AI.
- `POST /api/ai/trip/evaluate`: trip comfort scoring through Python AI.
- `POST /api/ai/content/analyze`: travel text analysis through Python AI.
- `GET /api/ai/trip/{id}/comfort`: latest stored comfort record for a saved trip.

See `docs/api/java-python-api.md` for the Python-facing contract.
