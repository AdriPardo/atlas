# Siguiente paso de implementación

## Estado del último incremento (completado)

**v0.8 Cron schedules** ya está en el árbol:

- Entidad `CronJob` (SYNC_HOST / BACKUP_DATABASE) + Flyway `V15` + CRUD `/api/v1/cron-jobs`.
- Tick scheduler (`atlas.cron.poll-interval-ms`) encola jobs existentes vía `SyncHostUseCase` / `EnqueueBackupDatabaseUseCase`.
- UI `/cron`: listar/crear/enable/disable.

**Previo:** v0.7 VIEWER/DEVELOPER matrix; Alerts; Domains; backups; retention.

## Recomendación única (siguiente)

**v0.8 remainder — Hardening docs / restore runbook** — documentar backup restore de prueba y runbooks ops sin abrir billing.

## Por qué es el paso más rentable ahora

1. Cron + backup + retention cubren resilience runtime; falta evidencia operativa.
2. Restore UI completa sigue aplazable; un runbook desbloquea el criterio done de v0.8.
3. v0.9 billing puede esperar a que ops docs estén listos.

## Alcance concreto del incremento

1. Ampliar `docs/deployment/backup-restore.md` con restore de prueba documentado.
2. Runbook corto: cron, alerts, domains verify, SSO/CI intactos.
3. Enlace desde README si falta.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka, no Cloudflare API real, no restore UI completa.

## Definición de éxito

> Un operador puede seguir el runbook y restaurar un dump de prueba; cron/backup/RBAC intactos.
