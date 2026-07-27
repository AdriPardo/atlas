# Siguiente paso de implementación

## Estado del último incremento (completado)

**v0.8b — Backups DB programados** ya está en el árbol:

- Job `BACKUP_DATABASE` + handler `pg_dump` → `$ATLAS_BACKUP_DIR/atlas-*.sql.gz`.
- Config `atlas.backup.*` (enabled, dir, keep-count, cron).
- Scheduler diario (`DatabaseBackupScheduler`, default 02:30 UTC).
- ADMIN: `POST /api/v1/admin/backup` (202 + job).
- Runbook: `docs/deployment/backup-restore.md`.

**Previo:** v0.8a retention/purge; v0.6.1 Git webhooks.

## Recomendación única (siguiente)

**v0.7 remainder — Domains + Traefik metadata** — registrar dominios/certs por project y adapters básicos (cierra edge multi-tenant tras ACL).

## Por qué es el paso más rentable ahora

1. Continuidad (purge + backup) ya cubierta.
2. Domains desbloquean el resto de v0.7 (alerts pueden esperar).
3. Billing/AI siguen fuera de alcance.

## Alcance concreto del incremento

1. Entidad Domain + API CRUD mínima por project (ADMIN/OPERATOR con ACL).
2. Metadata certificados + adapter Traefik/Cloudflare stub o lectura de labels.
3. UI mínima: listar/añadir dominio en project detail.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka, no restore UI completa aún.

## Definición de éxito

> Project puede registrar un dominio verificado (metadata); webhooks/RBAC/purge/backup intactos.
