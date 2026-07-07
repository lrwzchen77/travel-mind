# Java-Python REST Response Format

Java calls the Python AI service through HTTP REST. Python endpoints should return a stable JSON envelope so Java can parse success and failure consistently.

## Envelope

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "traceId": "optional-request-trace-id",
  "timestamp": 1760000000000
}
```

## Rules

- `code = 0` means success; non-zero values mean business or service failure.
- `message` is a short readable status.
- `data` contains endpoint-specific payloads and may be `null` on failure.
- `traceId` is optional in Phase 1 and should be forwarded when Java provides it.
- `timestamp` is Unix epoch milliseconds when available.
- Python service health is exposed at `GET /health`.

## Failure Example

```json
{
  "code": 50001,
  "message": "python ai service unavailable",
  "data": null
}
```
