# Dominio — Modelo

## Agregados principales (objetivo)

```text
Organization (1 por install)
  └── Team ──< Membership >── User
  └── Project
        └── Service
              ├── Deployment*  ── Host
              ├── Variable* / SecretRef*
              └── Domain?
        └── Pipeline ── PipelineRun ── Deployment?
  └── Host
        └── ContainerSnapshot* (proyección)
  └── Repository
  └── AlertRule / NotificationChannel
  └── AuditEntry* (append-only)
  └── Job*
  └── UsageRecord*
```

`*` = alta cardinalidad.

## Entidades y campos esenciales

### Organization
`id`, `name`, `slug`, `settings` (JSON), `created_at`

### User
`id`, `username`, `email?`, `password_hash`, `role` (legacy) / `roles[]`, `authentik_uid?`, `provider`, `disabled`, `created_at`

### Team / Membership
`Team(id, name, slug)` · `Membership(team_id, user_id, role_in_team)`

### Project (← Application)
`id`, `organization_id`, `name`, `slug`, `description`, `status`, `created_by`, timestamps

### Service (← campos deployables de Application)
`id`, `project_id`, `name`, `repository_id?`, `repository_url`, `branch`, `compose_path`, `domain`, `preferred_host_id?`, `environment`, `status`, timestamps

### Host
`id`, `hostname`, `ip`, `os`, `docker_version`, `online`, `labels` JSON, `secret_ref?`, timestamps

### Deployment
`id`, `service_id`, `host_id`, `pipeline_run_id?`, `status`, `git_sha?`, `started_at`, `finished_at`, `logs_ref`, `triggered_by`, timestamps

### Repository
`id`, `url`, `provider`, `credential_secret_id`, timestamps

### Pipeline / PipelineRun / PipelineStep
Definición + ejecución + pasos con status

### Job
Ver [workers-queues.md](../architecture/workers-queues.md)

### Secret / Variable
`scope` (ORG|PROJECT|SERVICE), `key`, `value`/`ciphertext`, `version`

### Domain / Certificate / DnsRecord / TraefikRoute
Networking desired state

### AlertRule / NotificationChannel / NotificationDelivery
Observability producto

### Backup
Data protection

### AuditEntry
`actor_user_id`, `action`, `resource_type`, `resource_id`, `metadata`, `created_at`

### UsageRecord
`meter`, `quantity`, `period_start`, `period_end`, `dimensions` JSON

## Enums relevantes

- `ApplicationStatus` MVP → map a `ProjectStatus` / `ServiceStatus`
- `DeploymentStatus` ampliado con `QUEUED`, `CANCELLED`, `ROLLED_BACK`
- `JobStatus`: `PENDING|RUNNING|SUCCEEDED|FAILED|DEAD|CANCELLED`
- `Role`: `ADMIN|OPERATOR|VIEWER|DEVELOPER` (+ custom later)

## Relaciones MVP → objetivo

| Tabla hoy | Destino |
|-----------|---------|
| `applications` | `projects` + `services` (migración Flyway) |
| `hosts` | `hosts` + columnas labels/credentials |
| `deployments` | `deployments` FK a `services` (map desde application_id) |
| `users` | `users` enriquecido + `memberships` |

## Invariantes

- Nombre/slug único de Project por Organization.
- Un Deployment pertenece a un Service y un Host existentes.
- No borrar Host/Service con Deployments activos (RESTRICT / soft-delete).
- Secrets nunca se loguean ni se emiten en eventos.
- Single Organization row en v0–v1.
