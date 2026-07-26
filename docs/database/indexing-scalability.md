# Database — Indexing & scalability

## Objetivo

Soportar **miles de projects**, decenas/cientos de usuarios, alto volumen de `deployments`, `jobs`, `audit_entries` en **una** instalación single-tenant.

## Índices por tabla caliente

### projects
- `UNIQUE (organization_id, slug)`
- `(organization_id, created_at DESC)`
- `(organization_id, status)`
- `LOWER(name)` search opcional vía `pg_trgm` si `q=` es frecuente

### services
- `(project_id)`
- `(status)`
- `UNIQUE (project_id, name)`

### deployments
- `(service_id, created_at DESC)` — listado por service
- `(host_id, created_at DESC)`
- `(status, created_at DESC)` — dashboard failing
- `(created_at DESC)` — global feed

### jobs
- partial index `WHERE status = 'PENDING'` on `(available_at, created_at)`
- `(resource_type, resource_id)`
- `(locked_by) WHERE status = 'RUNNING'`

### audit_entries
- `(created_at DESC)`
- `(actor_user_id, created_at DESC)`
- `(resource_type, resource_id, created_at DESC)`
- Particionar por rango de tiempo cuando >10–50M filas (v1+)

### hosts
- `UNIQUE (hostname)`
- `(online)`
- GIN on `labels` si filtrado por label

### secrets/variables
- `UNIQUE (scope, scope_id, key)`

## Estrategias a escala

| Técnica | Cuándo |
|---------|--------|
| Paginación keyset en audit/logs | Listas deep |
| Archivar deployments logs a object storage / Loki | `logs` TEXT crece |
| Particiones mensuales `audit_entries`, `usage_records` | Retención larga |
| Read replica Postgres | Reportes pesados (raro en self-host single node) |
| Cache Redis listados dashboard | Hot path |
| Connection pool (Hikari) sizing | Workers + API |

## Multi-user concurrency

- Transactions cortas en API.
- Jobs `SKIP LOCKED` evita contención entre workers.
- Advisory lock `hashtext(service_id)` durante deploy para serializar releases del mismo service.
- Optimistic locking (`version` column) en Settings/Secrets si ediciones concurrentes.

## Estimación de tamaño (orden de magnitud)

| Entidad | 5k projects | Notas |
|---------|-------------|-------|
| projects+services | ~5–15k rows | trivial |
| deployments / año | 0.5–5M | index + retention |
| audit | 1–10M | partition |
| jobs | churn alto | purge SUCCEEDED > N days |

## Vacuum / mantenimiento

- Autovacuum agresivo en `jobs`, `deployments`.
- Job de purge: jobs terminales > 30d; audit según policy.
