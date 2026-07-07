# Travel Mind Python AI

FastAPI service boundary for local AI capabilities.

```bash
python -m venv .venv
.venv/Scripts/python.exe -m pip install -r requirements.txt
.venv/Scripts/python.exe -m pytest
.venv/Scripts/python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 19080
```

The Phase 1 health endpoint is `GET /health`. AI algorithm endpoints are added in Phase 4.
