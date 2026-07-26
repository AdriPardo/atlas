# Siguiente paso de implementación

## Estado del último incremento (completado)

**v0.6.1 — Git webhooks** ya está en el árbol:

- Flyway V11: `pipelines.webhook_token` único.
- Público: `POST /api/v1/webhooks/git/{token}` → `RunPipelineUseCase.executeTrusted` → DEPLOY_SERVICE.
- Auth: token de path; HMAC GitHub (`X-Hub-Signature-256`) / Gitea (`X-Gitea-Signature`) si el header viene (secret = token Atlas).
- Rate limit in-memory ~30/min por token (configurable); 429 si se excede.
- UI pipeline detail: URL + copiar + rotar token (`POST /pipelines/{id}/webhook-token/rotate`).
- Audit: `PIPELINE_RUN` (triggeredBy=webhook) + `PIPELINE_WEBHOOK_ROTATE`.

## Recomendación única (siguiente)

**v0.8a Job retention + purge** — política de retención de jobs/pipeline_runs antiguos + endpoint/cron de purge (cierra continuidad operativa sin el alcance completo de backups).

## Por qué es el paso más rentable ahora

1. GitOps ligero (pipelines + webhooks) ya dispara deploys; la cola crece sin retención.
2. Alcance pequeño vs backups volúmenes/DB completos (resto v0.8).
3. LOCAL deploy hardening / docs CI pueden ir en paralelo; purge desbloquea salud de prod.

## Alcance concreto del incremento

1. Settings/config: retención días (jobs + pipeline_runs).
2. Use case + worker tick o endpoint ADMIN `POST /api/v1/admin/purge`.
3. Tests de purge + doc corta en next-step.

## Qué no hacer

- No billing/AI, no Redis/Kafka, no Traefik domains completos, no restore de volúmenes aún.

## Definición de éxito

> Jobs/runs antiguos se eliminan según política; API/UI siguen verdes; webhooks y RBAC intactos.
