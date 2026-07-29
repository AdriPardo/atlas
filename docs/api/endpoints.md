# API — Endpoints

Leyenda versión: **MVP** | **v0.2+** | **v0.4+** | **v0.5+** | **v0.6+** | **v0.7+** | **v0.9+** | **v1.0**

## Auth & me

| Método | Path | Versión | Notas |
|--------|------|---------|-------|
| POST | `/auth/login` | MVP | local |
| GET/POST | `/auth/sso` | MVP | Authentik headers |
| POST | `/auth/logout` | v0.5 | opcional blacklist/jti |
| GET | `/me` | MVP | |
| GET | `/dashboard/stats` | MVP | ampliar campos |

## Catalog / Delivery

| Método | Path | Versión |
|--------|------|---------|
| CRUD | `/applications` | MVP (deprecated → projects) |
| CRUD | `/projects` | v0.2 |
| CRUD | `/projects/{id}/services` | v0.2 |
| GET/POST | `/services` | v0.2 |
| CRUD | `/repositories` | v0.5 |
| CRUD | `/pipelines` | v0.6 |
| GET | `/pipelines/{id}/runs` | v0.6 |
| POST | `/pipelines/{id}/runs` | v0.6 |
| CRUD | `/deployments` | MVP |
| POST | `/deployments/{id}/cancel` | v0.4 |
| POST | `/deployments/{id}/retry` | v0.4 |
| GET | `/deployments/{id}/logs` | v0.4 (stream/chunk) |
| POST | `/services/{id}/deploy` | v0.4 | 202 + deployment; body opcional `hostId`, `exposure`, `placementMode` (SHARED\|ISOLATED) |

## Runtime

| Método | Path | Versión |
|--------|------|---------|
| CRUD | `/hosts` | MVP |
| POST | `/hosts/{id}/sync` | v0.4 |
| GET | `/hosts/{id}/containers` | v0.5 |
| POST | `/containers/{id}/restart` | v0.5 |
| CRUD | `/volumes` | v0.8 |
| CRUD | `/cron-jobs` | v0.8 |

## Config

| Método | Path | Versión |
|--------|------|---------|
| CRUD | `/variables` | v0.4 |
| CRUD | `/secrets` | Org/global (ADMIN create); v0.4+ |
| GET/POST | `/projects/{id}/secrets` | Project-owned + list linked; v0.8+ |
| POST/DELETE | `/projects/{id}/secrets/bindings` | Link/unlink global secret; v0.8+ |
| POST | `/secrets/{id}/reveal` | v0.4 (planned) |

## Networking

| Método | Path | Versión |
|--------|------|---------|
| CRUD | `/domains` | v0.7 |
| POST | `/domains/{id}/verify` | v0.7 |
| GET | `/domains/{id}/tunnel-ingress` | v0.8.1 Autopilot |
| POST | `/domains/{id}/tunnel-ingress/ensure` | v0.8.1 Autopilot |
| GET | `/domains/{id}/dns-cname` | v0.8.4 Autopilot |
| POST | `/domains/{id}/dns-cname/ensure` | v0.8.4 Autopilot |
| GET | `/certificates` | v0.7 |
| CRUD | `/dns-records` | v0.7 |
| GET/PUT | `/traefik/routes` | v0.7 |

## Observability

| Método | Path | Versión |
|--------|------|---------|
| GET | `/logs/query` | v0.5 |
| GET | `/metrics/query` | v0.5 |
| CRUD | `/alerts` | v0.7 |
| POST | `/alerts/{id}/silence` | v0.7 |
| CRUD | `/notification-channels` | v0.7 |

## Identity & platform

| Método | Path | Versión |
|--------|------|---------|
| GET/PATCH | `/users` | v0.7 |
| CRUD | `/teams` | v0.7 |
| GET | `/audit` | v0.7 |
| GET/PATCH | `/settings` | v0.4 |
| GET | `/billing/usage` | v0.9 |
| GET | `/billing/entitlements` | v0.9 |
| CRUD | `/backups` | v0.8 |
| POST | `/backups/{id}/restore` | v0.8 |

## Webhooks & jobs

| Método | Path | Versión |
|--------|------|---------|
| POST | `/webhooks/git/{token}` | v0.6 (filtro push+branch v0.8.7; Autopilot host v0.8.12) |
| POST | `/pipelines/enable-auto-deploy` | v0.8.7 (`hostId` opcional; default sin pin v0.8.12) |
| GET | `/jobs` | v0.4 |
| GET | `/jobs/{id}` | v0.4 |

## Actuator (no versionado bajo /api)

`GET /actuator/health`, `/actuator/info`, (métricas scrape) `/actuator/prometheus` v0.5.
