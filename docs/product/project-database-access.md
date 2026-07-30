# Producto — Acceso DB por Project

Contrato de producto para exponer acceso a base de datos desde Atlas (ops plane). Decisión vinculante: [ADR-0015](../decisions/ADR-0015-project-database-access.md).

## Intento

Cada Project/Service ve **solo** su namespace de datos. Permisos controlados (read / migrate / admin). Compatible con Autopilot, `atlas.yml` y migrators del repo — **sin** acoplar Atlas a Prisma u otro ORM.

## Modelo de aislamiento

| Capa | Qué |
|------|-----|
| Control plane | DB `atlas` — solo Atlas. Nunca en secretos de project. |
| Customer data | Schema `app_<project_slug>` (default) o database dedicada (opt-in). |
| Identity DB | Rol Postgres por project (+ opcional rol read-only). |
| Entrega | Secret lógico `db.url` (y opcional `db.schema`) en el project. |

Install Atlas = una org ([ADR-0002](../decisions/ADR-0002-single-tenant-install.md)). Muchos projects dentro = muchos schemas/roles en el **mismo** cluster Postgres compartido del homelab/org, o DBs dedicadas si el riesgo lo pide.

## Permisos (defaults)

| Quién | Perfil | Qué puede |
|-------|--------|-----------|
| Contenedor / `migrateCommand` | `db.migrate` | DDL+DML en schema propio |
| Humano “ver datos” | `db.read` | Solo SELECT |
| Break-glass | `db.admin` | Ownership schema; audit obligatorio |

## Opciones de acceso (producto)

1. **Hoy:** operador crea schema/rol fuera de Atlas; guarda connection string como secret project `db.url`; Compose/`atlas.yml` `envFrom.secretRef: db.url` → deploy escribe `DATABASE_URL` en `.env`.
2. **Hoy (slice 1):** provisioner Atlas (`POST /projects/{id}/database/provision`) CREATE ROLE/SCHEMA + grants + UI “Database” en Project detail (metadata + secrets `db.url` / `db.schema`). Requiere `ATLAS_APP_DB_URL` apuntando a DB dedicada (p. ej. `apps`), **nunca** `atlas`.
3. **Hoy (opción C):** emitir credenciales / URLs con TTL (`POST /projects/{id}/database/credentials`) para consola local (`psql`, GUI). Default `db.read`; `db.migrate` / `db.admin` con permisos más altos. Revoke temprano vía DELETE. No rota `db.url`.
4. **Diferido:** SQL console proxy con RLS (opción B) — alto riesgo ops.

## Convención de secretos

Ver [config-security.md](../modules/config-security.md). Nombres: `db.url`, `db.schema`, `db.password` (legacy).

## Qué no hacer

- No mezclar schemas de apps con DB `atlas` / Authentik / Grafana.
- No hardcodear Prisma en Atlas; solo `runtime.migrateCommand` o migrate-on-start del contenedor ([app-migrations.md](../deployment/app-migrations.md)).
- No exponer `ATLAS_DB_*` ni master key a projects.
- No tocar `.env` de Reelpath desde este flujo sin ownership explícito del project.
