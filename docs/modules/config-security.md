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

### API

| Método | Ruta | Notas |
|--------|------|-------|
| GET/POST | `/secrets` | Org/global; POST = ADMIN |
| GET/POST | `/projects/{id}/secrets` | List (owned+linked) / create owned |
| POST | `/projects/{id}/secrets/bindings` | Link global → alias |
| DELETE | `/projects/{id}/secrets/bindings/{bindingId}` | Unlink |
| DELETE | `/projects/{id}/secrets/{secretId}` | Delete owned |

UI: panel **Secrets** en Project detail; página sidebar **Org secrets** (`/secrets`) para el almacén compartido.

Nombres lógicos conocidos (hints en UI):

| Nombre | Uso | Scopes / notas |
|--------|-----|----------------|
| `git.token` | Clone privado + registro webhook GitHub | PAT GitHub con scope `repo` |
| `cloudflare.api.token` | Autopilot PUBLIC Tunnel + DNS CNAME | **Zone → DNS → Edit** + **Account → Cloudflare Tunnel / Cloudflare One → Edit** (un solo token basta) |

Rotación: crear nueva versión / reemplazar valor; deploys siguientes usan latest.
