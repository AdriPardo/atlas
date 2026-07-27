# Siguiente paso de implementación

## Estado del último incremento (completado)

**v0.8a — Job retention + purge** ya está en el árbol:

- Config `atlas.retention.*` (enabled, jobs-days, pipeline-runs-days, cron).
- `PurgeRetentionUseCase`: borra pipeline_runs terminales y luego jobs terminales más antiguos que N días.
- Scheduler diario (`RetentionPurgeScheduler`, default 03:00).
- ADMIN: `POST /api/v1/admin/purge`.
- Audit `RETENTION_PURGE`.

**Previo:** v0.6.1 Git webhooks (`POST /api/v1/webhooks/git/{token}`).

## Recomendación única (siguiente)

**v0.8b Backups DB programados** — snapshot lógico Postgres (pg_dump) vía job + restore documentado (cierra continuidad tras purge).

## Por qué es el paso más rentable ahora

1. Purge reduce churn; falta backup antes de operar en serio.
2. Alcance acotado vs restore de volúmenes Docker.
3. Domains/Traefik (resto v0.7) y billing pueden esperar.

## Alcance concreto del incremento

1. Job type `BACKUP_DATABASE` + handler pg_dump a path configurable.
2. Schedule/cron config + UI mínima o endpoint ADMIN trigger.
3. Doc restore de prueba en runbook corto.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka, no Traefik domains completos aún.

## Definición de éxito

> Backup programado o manual produce artefacto recuperable; restore de prueba documentado; webhooks/RBAC/purge intactos.
