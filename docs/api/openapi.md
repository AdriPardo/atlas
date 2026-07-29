# OpenAPI contract

Atlas publishes an OpenAPI 3 contract for external clients and codegen.

## Artifact

Committed snapshot: [`openapi.json`](./openapi.json).

Regenerate after controller/DTO changes:

```bash
cd backend
./gradlew :bootstrap:test --tests com.atlas.OpenApiContractIntegrationTest -Datlas.writeOpenApi=true
```

CI/local test (without write) asserts:

- Required paths exist (`/projects`, `/services`, `/hosts`, `/applications`, `/auth/login`).
- `/applications` operations are marked `deprecated: true`.
- Committed snapshot paths are still present in the live `/v3/api-docs` document.

## Live docs (non-prod)

Springdoc is permitted only on profiles `local`, `docker`, and `test` ([SecurityConfig](../../backend/bootstrap/src/main/java/com/atlas/api/config/SecurityConfig.java)):

| URL | Purpose |
|-----|---------|
| `/v3/api-docs` | Raw OpenAPI JSON |
| `/swagger-ui.html` | Interactive UI |

Production should rely on the committed snapshot (or protect docs behind auth) — live Swagger is not exposed on other profiles.

## Auth

Security scheme: HTTP Bearer JWT (`bearerAuth`). Obtain a token via `POST /api/v1/auth/login` or SSO.

## Deprecations

See [deprecations.md](./deprecations.md). `/api/v1/applications` remains until Sunset **2027-08-01**; prefer `/projects` + `/services`.
