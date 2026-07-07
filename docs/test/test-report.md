# Test Report

## Summary

Phase 5 verification was run on 2026-07-07 in the local Windows/Scoop/Docker environment. MySQL and Redis ran in Docker, Spring Boot ran on port `8080`, Vue/Vite on `5173`, and FastAPI on `19080`.

## Automated Results

| Command | Result |
| --- | --- |
| `python-ai/.venv/Scripts/python.exe -m pytest -q` | Passed: 5 tests. |
| `mvn test` | Passed: backend reactor build success; trip, resource, and controller tests passed. |
| `mvn -pl app -am package -DskipTests` | Passed: Spring Boot jar repackaged successfully. |
| `cd frontend && npm test` | Passed: 4 files, 7 tests. |
| `cd frontend && npm run build` | Passed: Vite production build generated `dist`. |

## Integration Smoke Results

| Check | Evidence |
| --- | --- |
| Backend service | `GET /health` returned `status=healthy`. |
| Python AI service | `GET :19080/health` returned `status=healthy`. |
| MySQL and Redis | Docker containers `travel-mind-mysql` and `travel-mind-redis` were listening on `3306` and `6379`. |
| Frontend routes | `/`, `/planning`, `/trip-history`, `/ai-lab`, `/ai-records`, and `/trip/1783410670275200` returned HTTP 200. |
| Profile flow | `GET` and `PUT /api/users/profile?userId=1001` returned HTTP 200; nickname was restored after smoke. |
| CRUD flow | Created, read, updated, and soft-deleted city `1783410664276503`. |
| Trip planning | Task `90f5e4b7` completed and saved plan `1783410666254761`. |
| Trip history | `GET /api/trip/history?limit=3` returned recent saved plans. |
| Image detection | Java `POST /api/ai/vision/detect` returned `success=true` and persisted an AI record. |
| Trip comfort scoring | Java `POST /api/ai/trip/evaluate` returned score `76`; saved plan `1783410666254761` returned a successful comfort record. |
| Text analysis | Java `POST /api/ai/content/analyze` returned `success=true` with `sentiment=mixed`. |
| Java-Python failure fallback | With Python stopped, Java AI returned `data.success=false` and stored failed records. |
| Trip planning with Python down | Task `8b03dafb` completed and saved plan `1783410670275200`; comfort scoring failure did not block planning. |
| Large-model fallback | Logs showed `aiAvailable=false` and MySQL Demo Planner completed the trip when external runtime settings were missing. |

## Residual Risks

- Real map, XHS, and large-model providers were not called because local credentials were intentionally absent.
- Optional YOLO mode was not exercised because no local `TRAVEL_MIND_YOLO_MODEL` was configured.
