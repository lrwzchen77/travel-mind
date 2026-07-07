# Defect Fix Log

| Date | Defect | Root Cause | Fix | Verification |
| --- | --- | --- | --- | --- |
| 2026-07-07 | App package failed because jar could not be renamed. | Running Java process locked `app-0.0.1-SNAPSHOT.jar`. | Stopped process on port `8080` before packaging. | `mvn -pl app -am package -DskipTests` passed. |
| 2026-07-07 | Java-Python smoke returned HTTP 500/422. | Java `HttpClient` attempted HTTP/2 upgrade and sent null fields rejected by FastAPI/Pydantic. | Forced HTTP/1.1 and omitted null request fields. | Java AI endpoints returned `success=true`; regression tests added. |
| 2026-07-07 | Python unavailable during trip scoring. | FastAPI service intentionally stopped for fallback test. | Existing non-blocking fallback persisted failed AI record and allowed planning to complete. | Task `8b03dafb` completed with saved plan `1783410670275200`. |
