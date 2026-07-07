# Git Workflow

## Branches

- `main`: stable delivery branch.
- `develop`: integration branch.
- `feature/backend-core`: backend foundation and orchestration.
- `feature/frontend-vue`: Vue application.
- `feature/python-ai`: FastAPI and local AI service.
- `feature/database-crud`: SQL and CRUD work.
- `feature/docs-test`: agile, test, and delivery documents.

## Authors

Use these author identities when creating team commits:

```text
Wang-ke-li <Wang-ke-li@users.noreply.gitee.com>
Chen Wenzhe <chen-wenzhe@example.local>
Zhu Qicheng <zhu-qicheng@example.local>
Zhang Shuai <zhang-shuai@example.local>
```

## Commit Messages

Use `type(scope): concise action`.

Allowed types: `feat`, `fix`, `docs`, `test`, `chore`, `style`, `ci`.

Examples:

```text
feat(backend): add unified response handling
feat(frontend): initialize vite application shell
docs(api): define java python response format
```
