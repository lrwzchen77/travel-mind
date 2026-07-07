# Release Notes v1.0.0

## Highlights

- Delivered Spring Boot + Vue + FastAPI + MySQL + Redis full-stack travel planning system.
- Added MySQL-backed CRUD for travel resources, user preferences, favorites, notes, trip plans, and AI records.
- Added async trip planning with progress polling, saved trip history, trip detail, copy, delete, and chat.
- Added local Python AI capabilities for image detection, trip comfort scoring, and travel text analysis.
- Connected Java to Python through REST with timeout handling and persistent audit records.
- Added Vue AI Lab, AI records page, and trip comfort display.
- Added graceful fallback for missing large-model settings and unavailable Python AI service.

## Verification

- Python tests: `5 passed`.
- Maven tests: reactor `BUILD SUCCESS`.
- Backend package: Spring Boot jar repackaged successfully.
- Frontend tests: `4` files and `7` tests passed.
- Frontend build: Vite production build succeeded.
- Integration smoke: backend, frontend, Python AI, MySQL, Redis, CRUD, trip planning, AI endpoints, and failure fallbacks verified locally.

## Known Limits

- Real map, XHS, and large-model providers require local credentials.
- Optional YOLO mode requires a local model path and additional dependency setup.
