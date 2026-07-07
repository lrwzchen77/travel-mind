# Repository Guidelines

## Project Structure & Module Organization
Travel Mind is a full-stack project. The Maven backend root aggregates `common`, `modules`, and `app`. `app/` contains HTTP/WebSocket APIs under `app/src/main/java/com/zkry/api`; shared infrastructure lives in `common/*`; business capabilities live in `modules/*`. `frontend/` is the Vue 3 + Vite app shell. `python-ai/` is the FastAPI service. `sql/` stores MySQL initialization scripts, and `docs/` contains design, API, agile, test, and deploy material.

## Build, Test, and Development Commands
- `mvn clean package` builds every module and creates the application jar.
- `mvn test` runs the Maven test phase for the full reactor.
- `mvn -pl app -am spring-boot:run` starts the backend locally and also builds required reactor modules.
- `mvn -pl modules/trip -am test` runs checks for one module plus dependencies; replace `modules/trip` with another module path as needed.
- `cd frontend && npm test && npm run build` checks the Vue shell.
- `cd python-ai && .venv/Scripts/python.exe -m pytest` runs FastAPI tests.

Copy `.env.example` to `.env` or export equivalent environment variables before running locally. The dev profile expects MySQL and Redis configuration.

## Coding Style & Naming Conventions
Use Java 17 features conservatively and keep packages under `com.zkry`. Follow four-space Java indentation, constructor injection, SLF4J logging, and Spring stereotypes. Vue files use PascalCase components and lower camel case exports. Python modules use snake_case and keep FastAPI route functions small.

## Testing Guidelines
Add Java tests under each module's `src/test/java`, mirroring the production package. Frontend tests live beside source files as `*.test.js`. Python tests live in `python-ai/tests` and use `test_*.py`. Run `mvn test`, `npm test`, and `pytest` for affected areas before opening a pull request.

## Commit & Pull Request Guidelines
Use `type(scope): concise action`, with types `feat`, `fix`, `docs`, `test`, `chore`, `style`, or `ci`. Pull requests should include purpose, affected modules, configuration changes, test results, and screenshots or sample API/WebSocket payloads when behavior changes.

## Security & Configuration Tips
Never commit real API keys, cookies, `.env`, logs, or generated runtime settings. Keep secrets in environment variables matching `.env.example`, and document any new required setting there.
