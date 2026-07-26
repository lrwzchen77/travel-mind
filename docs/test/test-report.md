# Test Report

## Summary

Final verification was repeated on 2026-07-26 with JDK 17.0.19, Python 3.12.10, MySQL 8.0.46, Redis 7, Qdrant 1.15.4, Spring Boot 3.5.10, and Vue 3.

## Automated Results

| Command | Result |
| --- | --- |
| `python-ai/.venv/Scripts/python.exe -m pytest` | Passed: 20 tests, including bundled YOLO, TravelComfort, memory analysis and Qdrant contract checks. |
| `mvn test` | Passed: 89 Java tests. |
| `mvn -pl app -am package -DskipTests` | Passed: executable Spring Boot jar generated. |
| `cd frontend && npm test` | Passed: 37 files, 115 tests. |
| `cd frontend && npm run build` | Passed: Vite production build generated `dist`. |

## Integration Results

| Check | Evidence |
| --- | --- |
| Runtime versions | MySQL returned `8.0.46`; Redis returned `PONG`; Java, Python and Qdrant health endpoints returned HTTP 200. |
| Database | `travelmind` contained 25 application tables and seed resources. |
| Redis business path | `GET /api/public/travel-map` stores the final POI-enriched snapshot under `travelmind:public-map:*` for 15 minutes; Redis failure falls back to the source path. |
| Unified POI content | All 34 system cities are covered. Admin `map-pois` returned 4,520 active AMap rows: 2,988 attractions, 752 hotels and 780 restaurants. A temporary admin import was stored as `source=manual`, visible publicly, rejected invalid coordinates with HTTP 400, then removed successfully. |
| AI readiness | Python `/health` reports YOLO, TravelComfort, memory embedding and Qdrant readiness separately and marks rule fallback or unavailability without taking down the service. |
| Operations boundary | The admin portal exposes user preference records in addition to user accounts, travel resources, plans and AI records. |
| Memory boundary | A private travel memory can be created only after the owned trip `end_date`; future trips are rejected in both UI and backend. |
| Trained YOLO | Python and Java forwarding both returned `model_mode=trained_yolo`, label `crowded_scene`, confidence `0.9933`, with a risk hint. |
| Java-Python persistence | Java stored the successful vision result in `tm_ai_analysis_record`. |
| Large model API | Java Spring AI called `deepseek-v4-flash` successfully and returned a Chinese travel answer. |
| Full planning workflow | Research Agent, AMap tools, Planner Agent, Review Agent, Python comfort scoring, and MySQL persistence completed for plan `1784088459745678`. |
| Settings security | Anonymous `/api/settings` returns HTTP 401; secret fields are redacted from controller responses. |
| Vue desktop | The saved plan displayed city, budget, comfort score, graph metrics, 3D map, and daily route at 1440x900. |
| Vue mobile | The same plan displayed without overlap or horizontal overflow at 390x844. |

## Model Evidence

- Dataset size: 946 images across six travel/risk classes.
- Validation accuracy: 0.9681.
- Test accuracy: 0.9394.
- Bundled weights: `python-ai/models/travel-risk-yolo-best.pt`.
- Reports: `docs/ai/dataset-report.md` and `docs/ai/training-report.md`.

## Residual Limits

- XHS is disabled by default because its cookie is private and expires; disabling it no longer blocks large-model planning.
- AMap forecasts cover only near-term dates. Future itinerary dates are marked `待临近出发确认` instead of using unrelated forecasts.
- Authentication business flows remain intentionally minimal; protected settings require a valid Sa-Token session.
- Git author history is preserved as factual evidence and is not rewritten merely to manufacture a target contribution ratio.
