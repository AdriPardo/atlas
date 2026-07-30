# Módulos — Configuración y secretos

## Variables

Pares key/value por Project/Service/Environment (`PRODUCTION`, `STAGING`, …). Inyectadas al deploy como env file generado por el worker.

Herencia: Organization defaults → Project → Service (override).

## Secrets

Almacén cifrado at-rest (`ATLAS_SECRETS_MASTER_KEY`). La API **nunca** lista valores en claro.

### Modelo (proyecto + org)

| Ámbito | `secrets.project_id` | Quién gestiona |
|--------|----------------------|----------------|
| Organization / global | `NULL` | ADMIN crea; listado metadata para autenticados (picker SSH) |
| Project-owned | UUID del project | Membership OPERATOR+ (`DEPLOY`) |

Además, `project_secret_bindings(project_id, secret_id, alias)` enlaza un secret **global** en un proyecto bajo un alias lógico (p. ej. `git.token`).

### Resolución en deploy / Git

Orden para un nombre lógico (p. ej. `git.token`) en el contexto de un proyecto:

1. Binding del proyecto cuyo `alias` coincide
2. Secret owned del proyecto con ese `name`
3. Secret organization/global con ese `name`

Hosts SSH siguen resolviendo por `sshPrivateKeySecretId` (id), sin cascada por nombre.

### Inyección a Compose (deploy)

Si el manifiesto declara `envFrom.secretRef` (`runtime.envFrom` y/o `services.*.envFrom`), el worker escribe las keys en el `.env` del workspace **antes** de `compose up`:

| Secret lógico | Env key por defecto |
|---------------|---------------------|
| `db.url` | `DATABASE_URL` |
| `db.schema` | `DB_SCHEMA` |
| `db.password` | `DB_PASSWORD` |
| otro | `SCREAMING_SNAKE` (`.`/`-` → `_`) |

Override: `env:` o `as:` en el item. Valores **nunca** van a logs de deploy. Secret no resuelto → warn + skip.

### API

| Método | Ruta | Notas |
|--------|------|-------|
| GET/POST | `/secrets` | Org/global; POST = ADMIN |
| GET/POST | `/projects/{id}/secrets` | List (owned+linked) / create owned |
| POST | `/projects/{id}/secrets/bindings` | Link global → alias |
| DELETE | `/projects/{id}/secrets/bindings/{bindingId}` | Unlink |
| DELETE | `/projects/{id}/secrets/{secretId}` | Delete owned |
| GET | `/projects/{id}/database` | Metadata schema/status (sin credenciales) |
| POST | `/projects/{id}/database/provision` | CREATE ROLE/SCHEMA + upsert `db.url` / `db.schema` |
| GET/POST | `/projects/{id}/database/credentials` | List TTL roles / issue ephemeral URL (`db.read` default) |
| DELETE | `/projects/{id}/database/credentials/{role}` | Revoke TTL role early |

UI: panel **Database** + **Secrets** en Project detail; página sidebar **Org secrets** (`/secrets`) para el almacén compartido.

Provisioner (ADR-0015): `ATLAS_APP_DB_URL` / `ATLAS_APP_DB_USERNAME` / `ATLAS_APP_DB_PASSWORD` → DB compartida de apps (p. ej. `apps`). Rechaza database name `atlas`. Docker Compose crea `apps` en init.

Nombres lógicos conocidos (hints en UI):

| Nombre | Uso | Scopes / notas |
|--------|-----|----------------|
| `git.token` | Clone privado + registro webhook GitHub | PAT GitHub con scope `repo` |
| `cloudflare.api.token` | Autopilot PUBLIC Tunnel + DNS CNAME | **Zone → DNS → Edit** + **Account → Cloudflare Tunnel / Cloudflare One → Edit** (un solo token basta) |
| `db.url` | Connection string de la DB del project (p. ej. `DATABASE_URL` en Compose) | Preferido. Aislamiento: schema/rol propios — [ADR-0015](../decisions/ADR-0015-project-database-access.md) |
| `db.schema` | Nombre de schema canónico (`app_<project_slug>`) | Metadata / binding; no sustituye `db.url` |
| `db.password` | Password suelta si el repo no usa URL única | Legacy; preferir `db.url` |

Rotación: crear nueva versión / reemplazar valor; deploys siguientes usan latest.

Acceso DB por project (producto): [project-database-access.md](../product/project-database-access.md).
