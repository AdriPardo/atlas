# Database — Schema design

## Principios

- PostgreSQL 16 como source of truth.
- Flyway migraciones versionadas (ya V1–V4).
- UUID PKs.
- `TIMESTAMPTZ` everywhere.
- Soft-delete opcional (`deleted_at`) en Project/Service/Host a partir de v0.5; hard delete restringido.
- JSONB para labels/settings/metadata flexibles — no para datos que se filtran siempre (esos van columna).

## Schema actual (MVP)

```text
users(id, username, password_hash, role, created_at)
applications(id, name, description, repository_url, branch, compose_path, domain, status, created_at, updated_at)
hosts(id, hostname, ip, operating_system, docker_version, online, created_at, updated_at)
deployments(id, application_id→applications, host_id→hosts, status, started_at, finished_at, logs, created_at, updated_at)
```

## Schema objetivo (evolutivo)

### Identity

```text
organizations(id, name, slug, settings jsonb, created_at)
users(+ email, display_name, authentik_uid, provider, disabled, last_login_at)
teams(id, organization_id, name, slug, created_at)
team_members(team_id, user_id, role_in_team, PRIMARY KEY(team_id,user_id))
roles(id, name, is_system)
permissions(id, code)
role_permissions(role_id, permission_id)
user_roles(user_id, role_id)
```

### Catalog & delivery

```text
projects(id, organization_id, name, slug, description, status, created_by, created_at, updated_at, deleted_at)
services(id, project_id, name, repository_id null, repository_url, branch, compose_path,
         domain, preferred_host_id null, environment, status, created_at, updated_at, deleted_at)
repositories(id, organization_id, url, provider, credential_secret_id, created_at)
pipelines(id, project_id, name, definition jsonb, created_at, updated_at)
pipeline_runs(id, pipeline_id, status, triggered_by, started_at, finished_at)
deployments(id, service_id, host_id, pipeline_run_id null, status, git_sha,
            started_at, finished_at, logs_ref, triggered_by, created_at, updated_at)
```

### Runtime & jobs

```text
hosts(+ labels jsonb, credential_secret_id, last_seen_at)
jobs(id, type, payload jsonb, status, attempts, available_at, locked_by, locked_at,
     last_error, resource_type, resource_id, created_at, updated_at)
outbox_events(id, type, payload jsonb, created_at, published_at)
```

### Config

```text
variables(id, scope, scope_id, key, value, created_at, updated_at)
secrets(id, scope, scope_id, key, ciphertext, key_version, created_at, updated_at)
```

### Networking / obs / audit / billing / backups

Tablas `domains`, `certificates`, `dns_records`, `traefik_routes`, `alert_rules`,
`notification_channels`, `notification_deliveries`, `audit_entries`, `usage_records`,
`backups` — según módulos.

## Migración Application → Project/Service

Flyway ejemplo conceptual (no implementar aquí):

1. Crear `organizations`, seed row.
2. Crear `projects`, `services`.
3. Backfill: cada application → project + service (mismos ids opcionales o map table).
4. Recrear FK `deployments.service_id`.
5. Vista o synonym API sobre `applications` durante deprecación.
6. Drop `applications` en versión posterior.

## Integridad

- FK ON DELETE RESTRICT en deployments.
- Unique `(organization_id, slug)` projects; `(project_id, name)` services.
- Check constraints en status enums (o PostgreSQL ENUM / text + check).
