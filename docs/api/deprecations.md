# API — Deprecations

## `/api/v1/applications` → Projects + Services

| | |
|--|--|
| **Status** | Deprecated (still served) |
| **Sunset** | `Wed, 01 Jul 2027 00:00:00 GMT` |
| **Successor** | `/api/v1/projects` + `/api/v1/services` |
| **ADR** | [ADR-0004](../decisions/ADR-0004-application-to-project-evolution.md) |

### Headers on every `/applications` response

- `Deprecation: true`
- `Sunset: Wed, 01 Jul 2027 00:00:00 GMT`
- `Link: </api/v1/projects>; rel="successor-version", </api/v1/services>; rel="alternate"`

### Mapping

| Legacy (`/applications`) | Canonical |
|--------------------------|-----------|
| `POST /applications` | `POST /projects` (+ default service) |
| `GET /applications` | `GET /projects` |
| `GET /applications/{id}` | `GET /projects/{id}` |
| `PUT /applications/{id}` | `PUT /projects/{id}` (+ service fields via `/services`) |
| `DELETE /applications/{id}` | `DELETE /projects/{id}` |
| `POST /applications/{id}/deploy` | `POST /services/{id}/deploy` |

Do **not** remove the alias before Sunset. After Sunset, delete `ApplicationController` and update clients to Projects/Services only.

### OpenAPI

Live (local/docker/test): `GET /v3/api-docs` · Swagger UI `/swagger-ui.html`.

Published snapshot for external clients: [`openapi.json`](./openapi.json). See [`openapi.md`](./openapi.md).
