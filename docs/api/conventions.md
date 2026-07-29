# API — Convenciones

## Base

- Prefijo: `/api/v1`
- JSON UTF-8
- Auth: `Authorization: Bearer <jwt>` salvo login/sso y health
- Versionado por URL ([ADR-0008](../decisions/ADR-0008-api-versioning.md))

## Verbos y status

| Acción | Método | Status OK |
|--------|--------|-----------|
| List | GET collection | 200 |
| Get | GET id | 200 / 404 |
| Create | POST | 201 + `Location` |
| Update | PUT (full) / PATCH (parcial futuro) | 200 |
| Delete | DELETE | 204 |
| Action | POST `.../{id}/:action` | 202 si async (deploy) |

## Paginación

Query: `page` (0-based), `size` (default 20, max 100), `sort=field,dir`.

Respuesta (ya en MVP `PageResponse`):

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 123,
  "totalPages": 7
}
```

Cursor pagination (`?cursor=&limit=`) para audit/logs de alta cardinalidad (v0.7+).

## Filtros

Query params explícitos: `status`, `q` (search), `projectId`, `hostId`, `from`, `to`. Sin lenguajes de query arbitrarios en v0.x.

## Errores

Cuerpo estable:

```json
{
  "timestamp": "2026-07-27T00:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "name is required",
  "path": "/api/v1/projects",
  "traceId": "..."
}
```

Códigos: `VALIDATION_ERROR`, `NOT_FOUND`, `CONFLICT`, `UNAUTHORIZED`, `FORBIDDEN`, `CONFLICT_STATE`, `RATE_LIMITED`, `INTERNAL_ERROR`.

## Idempotencia

Headers opcionales `Idempotency-Key` en POST deploy (v0.4+). Replays 24h.

## Async

Acciones largas → `202 Accepted` + cuerpo `{ "deploymentId"|"jobId": "..." }` + poll GET.

## Seguridad API

- RBAC en método.
- Never return secret values in list/get (salvo reveal dedicado).
- OpenAPI live (`/v3/api-docs`, Swagger UI) solo en perfiles `local` / `docker` / `test`.
- Contrato publicado para clientes externos: [`openapi.json`](./openapi.json) — ver [`openapi.md`](./openapi.md).

## Compatibilidad Application → Project

Durante transición (hasta Sunset **2027-08-01**):

- `/api/v1/applications` permanece con headers `Deprecation` / `Sunset` / `Link`.
- `/api/v1/projects` (+ `/services`) canónico.
- Mapeo y path de retirada: [`deprecations.md`](./deprecations.md).
