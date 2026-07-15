# Release Notes v1.1.0

## Highlights

- Bundled and integrated the self-trained TravelRisk-YOLO model.
- Added real trained-model regression coverage and training evidence.
- Enabled automatic root `.env` loading in Java and Python.
- Verified the DeepSeek chat and full multi-agent trip planning workflows.
- Added Docker Compose for MySQL 8.0 and Redis 7.
- Fixed disabled XHS incorrectly forcing Demo Planner fallback.
- Fixed missing-weather normalization for future trips.
- Prevented Demo Planner from mixing resources from unrelated cities.
- Fixed frontend port and trip-detail response unwrapping.
- Redacted runtime secrets and protected settings with authentication.

## Verification

- Java: 24 tests passed.
- Python: 6 tests passed, including real YOLO inference.
- Frontend: 8 tests passed and production build succeeded.
- Full-stack plan: `1784088459745678` completed and rendered on desktop/mobile.
