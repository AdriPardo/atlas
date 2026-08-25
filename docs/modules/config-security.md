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
| `ai.openai` | `OPENAI_API_KEY` |
| `ai.openai.base_url` | `OPENAI_BASE_URL` |
| `ai.elevenlabs` | `ELEVENLABS_API_KEY` |
| `ai.deepseek` | `DEEPSEEK_API_KEY` |
| `ai.provider` | `AI_PROVIDER` |
| `ai.api_key` | `AI_API_KEY` |
| `ai.base_url` | `AI_BASE_URL` |
| `smtp.host` | `SMTP_HOST` |
| `smtp.port` | `SMTP_PORT` |
| `smtp.user` | `SMTP_USER` |
| `smtp.password` | `SMTP_PASSWORD` |
| `smtp.from` | `SMTP_FROM` |
| `smtp.tls` | `SMTP_TLS` |
| `mail.api_token` | `MAIL_API_TOKEN` |
| otro | `SCREAMING_SNAKE` (`.`/`-` → `_`) |

Override: `env:` o `as:` en el item. Valores **nunca** van a logs de deploy. Secret no resuelto → warn + skip.

### API

| Método | Ruta | Notas |
|--------|------|-------|
| GET/POST | `/secrets` | Org/global; POST = ADMIN |
| PUT | `/secrets` | Org/global **upsert** (UI rotate / seed) |
| DELETE | `/secrets/{id}` | Org/global delete; ADMIN (cascades bindings) |
| GET/POST | `/projects/{id}/secrets` | List (owned+linked) / create owned |
| PUT | `/projects/{id}/secrets` | Project-owned **upsert** (UI rotate / seed) |
| POST | `/projects/{id}/secrets/bindings` | Link global → alias |
| DELETE | `/projects/{id}/secrets/bindings/{bindingId}` | Unlink |
| DELETE | `/projects/{id}/secrets/{secretId}` | Delete owned |
| GET | `/projects/{id}/database` | Metadata schema/status (sin credenciales) |
| POST | `/projects/{id}/database/provision` | CREATE ROLE/SCHEMA + upsert `db.url` / `db.schema` |
| GET/POST | `/projects/{id}/database/credentials` | List TTL roles / issue ephemeral URL (`db.read` default) |
| GET | `/projects/{id}/database/credentials/{role}` | Revoke TTL role early |
| GET/POST | `/projects/{id}/mail` | Mail status / provision SMTP secrets |
| POST | `/projects/{id}/mail/send` | Send via platform relay (rate limited) |
| GET | `/settings/mail` | Platform SMTP metadata (no secrets) |

UI: panel **Database** + **Mail** + **Secrets** en Project detail; página sidebar **Org secrets** (`/secrets`) para el almacén compartido.

Provisioner (ADR-0015): `ATLAS_APP_DB_URL` / `ATLAS_APP_DB_USERNAME` / `ATLAS_APP_DB_PASSWORD` → DB compartida de apps (p. ej. `apps`). Rechaza database name `atlas`. El rol de `ATLAS_APP_DB_USERNAME` necesita `CREATEROLE` (no hace falta SUPERUSER). Docker Compose crea `apps` en init; en Postgres compartido de prod: `CREATE DATABASE apps;` + `ALTER ROLE <user> WITH CREATEROLE;` una vez.

Nombres lógicos conocidos (hints en UI):

| Nombre | Uso | Scopes / notas |
|--------|-----|----------------|
| `git.token` | Clone privado + registro webhook GitHub | PAT GitHub con scope `repo` |
| `cloudflare.api.token` | Autopilot PUBLIC Tunnel + DNS CNAME | **Zone → DNS → Edit** + **Account → Cloudflare Tunnel / Cloudflare One → Edit** (un solo token basta) |
| `db.url` | Connection string de la DB del project (p. ej. `DATABASE_URL` en Compose) | Preferido. Aislamiento: schema/rol propios — [ADR-0015](../decisions/ADR-0015-project-database-access.md) |
| `db.schema` | Nombre de schema canónico (`app_<project_slug>`) | Metadata / binding; no sustituye `db.url` |
| `db.password` | Password suelta si el repo no usa URL única | Legacy; preferir `db.url` |
| `ai.openai` | API key OpenAI / compatible → `OPENAI_API_KEY` | Usuario/ops en Atlas — [ADR-0017](../decisions/ADR-0017-platform-provided-ai.md) |
| `ai.openai.base_url` | Base URL OpenAI-compatible → `OPENAI_BASE_URL` | Local LLM / proxy; pair con `ai.openai` |
| `ai.elevenlabs` | API key ElevenLabs → `ELEVENLABS_API_KEY` | Usuario/ops (ADR-0017) |
| `ai.deepseek` | API key DeepSeek → `DEEPSEEK_API_KEY` | Usuario/ops (ADR-0017) |
| `ai.provider` | Selector lógico → `AI_PROVIDER` | `openai` \| `deepseek` \| `local` \| … |
| `ai.api_key` | Key genérica → `AI_API_KEY` | Single-client abstraction |
| `ai.base_url` | Base URL genérica → `AI_BASE_URL` | Local / gateway |
| `smtp.host` | Relay SMTP → `SMTP_HOST` | Platform provisioner — [ADR-0018](../decisions/ADR-0018-project-mail-access.md) |
| `smtp.port` | Puerto → `SMTP_PORT` | 1025 Mailpit / 587 TLS |
| `smtp.user` | Usuario SMTP → `SMTP_USER` | Cuando `ATLAS_APP_SMTP_AUTH=true` |
| `smtp.password` | Password → `SMTP_PASSWORD` | Por project tras provision |
| `smtp.from` | From default → `SMTP_FROM` | `{slug}@{fromDomain}` |
| `smtp.tls` | STARTTLS → `SMTP_TLS` | `true` / `false` |
| `mail.api_token` | Token API HTTP → `MAIL_API_TOKEN` | Header `X-Atlas-Mail-Token` |

Rotación: UI **Rotate value** / `PUT` upsert / [`scripts/seed-project-secrets.sh`](../../scripts/seed-project-secrets.sh) (bulk opcional desde `.env.secrets`). Deploys siguientes usan latest. Cualquier nombre lógico funciona; `ai.*` es solo convención de mapeo env.

Acceso DB por project (producto): [project-database-access.md](../product/project-database-access.md).
Secretos para apps (producto): [secrets-for-apps.md](../product/secrets-for-apps.md).
