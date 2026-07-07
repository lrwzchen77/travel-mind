# API Guidelines

## Naming

- Public backend APIs use `/api/{domain}/{resource}`.
- Use plural resource names for collections: `/api/trips`, `/api/cities`.
- Use path variables for identity: `/api/trips/{tripId}`.
- Use query parameters for filters and paging: `?keyword=beijing&page=1&pageSize=20`.
- Keep field names in lower camel case for Java-facing JSON.

## Methods

- `GET` reads data.
- `POST` creates resources or starts commands.
- `PUT` replaces or updates resources.
- `DELETE` removes resources.

## Errors

Backend errors should use the common `R` envelope with `code`, `message`, `data`, `traceId`, and `timestamp` where applicable. Validation failures must return actionable field messages.
