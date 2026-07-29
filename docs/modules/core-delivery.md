# Módulos — Core delivery

## Dashboard

**Propósito:** primera pantalla operativa: salud de flota, deploys recientes, fallos, hosts offline.

**MVP:** contadores (`applications`, `hosts`, `deployments`, online hosts).

**Objetivo:**
- Widgets: failing deploys 24h, queue depth, top projects by activity.
- Activity feed (eventos recientes).
- Deep-links a Alerts.

## Projects (ex Application)

**Propósito:** unidad de producto que el equipo gestiona (nombre, descripción, ownership).

Un Project contiene uno o más **Services**. Migración: cada `applications` row → `projects` + `services` 1:1 ([ADR-0004](../decisions/ADR-0004-application-to-project-evolution.md)).

Campos clave: `name`, `slug`, `description`, `status`, timestamps, `created_by`.

## Services

**Propósito:** unidad desplegable concreta (repo ref, cómo correr, dominio primario, host preferido).

Campos heredados del Application actual: `repositoryUrl`, `branch`, `composePath`, `domain`, `status`.

**Dirección (ADR-0014):** `composePath` es opcional (fase C): si el checkout tiene `atlas.yml` con `runtime.composeFile`, ese path gana; si no hay manifiesto, Atlas sintetiza uno mínimo desde `composePath`. Sin ambos, el deploy falla con mensaje claro. Autopilot no vive en el manifiesto (placement, DNS, secrets, exposure).

Estados: `REGISTERED` | `READY` | `DEPLOYING` | `RUNNING` | `DEGRADED` | `STOPPED` | `FAILED`.

## Repositories

**Propósito:** credenciales Git y webhooks independientes del service (un repo → muchos services).

- Provider: GitHub/GitLab/Gitea/generic.
- Deploy keys / PAT cifrados.
- Webhook endpoint Atlas: `POST /api/v1/webhooks/git/{token}` → encola pipeline.
- **v0.8.7:** filtro `push` + branch del service; one-click `POST /pipelines/enable-auto-deploy` (crea Pipeline + intenta registrar webhook GitHub si existe `git.token`).

## Pipelines

**Propósito:** definición declarativa de pasos (clone → build? → deploy → smoke).

- `Pipeline` (yaml/json almacenado o UI builder simple).
- `PipelineRun` + steps.
- v0.x: pipeline implícito “deploy only”; v0.6+: steps visibles.
- **Auto-deploy:** Project UI → “Enable auto-deploy” o API `POST /api/v1/pipelines/enable-auto-deploy`.

## Deployments

**Propósito:** intento concreto de publicar un Service en un Host (o environment).

**MVP:** CRUD manual, logs texto, status simulado.

**Objetivo:** creado por API/UI/webhook → Job `DEPLOY_SERVICE` → worker actualiza status/logs → eventos.

Estados: `PENDING` | `QUEUED` | `RUNNING` | `SUCCEEDED` | `FAILED` | `CANCELLED` | `ROLLED_BACK`.

Relaciones: `service_id`, `host_id`, `pipeline_run_id?`, `triggered_by`, `git_sha?`.
