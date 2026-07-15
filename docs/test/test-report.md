# Test Report

## Summary

Final verification was repeated on 2026-07-15 with JDK 17.0.19, Python 3.12.10, MySQL 8.0.46, Redis 7, Spring Boot 3.5.10, and Vue 3.

## Automated Results

| Command | Result |
| --- | --- |
| `python-ai/.venv/Scripts/python.exe -m pytest -q` | Passed: 6 tests, including real bundled YOLO inference. |
| `mvn test` | Passed: 24 Java tests. |
| `mvn -pl app -am package -DskipTests` | Passed: executable Spring Boot jar generated. |
| `cd frontend && npm test` | Passed: 4 files, 8 tests. |
| `cd frontend && npm run build` | Passed: Vite production build generated `dist`. |

## Integration Results

| Check | Evidence |
| --- | --- |
| Runtime versions | MySQL returned `8.0.46`; Redis returned `PONG`; Java and Python health endpoints returned HTTP 200. |
| Database | `travelmind` contained all 13 required tables and seed resources. |
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
