# Requirements Specification

## Product Goal

Travel Mind helps users generate, review, store, and manage personalized travel plans while keeping traditional CRUD features and local AI analysis visible in the product.

## Functional Requirements

- Users can view and update a profile and travel preferences.
- Users can manage travel resources: cities, attractions, hotels, restaurants, tags, favorites, travel notes, trip history, and AI records.
- Users can submit a trip request with destination, dates, days, transportation, accommodation, budget, preferences, free-text requirements, and language.
- The backend processes trip planning asynchronously and exposes task progress through polling.
- Generated trip plans are stored in MySQL and can be listed, opened, copied, deleted, and used for trip chat.
- Java integrates large-model planning and falls back to local MySQL demo planning when external settings are missing.
- Java calls Python AI through REST for image detection, trip comfort scoring, and travel text analysis.
- AI results and failures are stored in `tm_ai_analysis_record`.
- Vue displays planning, trip detail, trip history, CRUD pages, AI Lab, and AI record views.

## Non-Functional Requirements

- Backend target must be JDK 17 or lower; this project uses JDK 17.
- Frontend and backend remain separated.
- Python AI runs as a separate FastAPI service and must not access MySQL directly.
- Demo path must not require GPU, YOLO model, or paid external credentials.
- External provider failures must produce graceful fallback behavior.

## Acceptance Requirements

- Backend, frontend, Python AI, MySQL, and Redis run locally.
- Automated tests and final smoke checks pass.
- Deployment, API, design, agile, and test documents are present.
