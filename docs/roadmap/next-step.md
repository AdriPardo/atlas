# Siguiente paso de implementación

## Estado del último incremento (completado)

**v0.7 remainder — Project roles VIEWER/DEVELOPER** ya está en el árbol:

- Matriz `ProjectMemberRole` (VIEWER read / DEVELOPER write / OPERATOR deploy+members) con tests de dominio + auth.
- ACL en services CRUD, pipeline update/delete, delete project (DEPLOY), alert write (DEPLOY), secrets create (ADMIN).
- UI memberships: rol por defecto VIEWER, helper de matriz, cambio de rol inline.
- Integration test: VIEWER lee y no escribe; DEVELOPER crea service y no despliega/borra project.

**Previo:** v0.7 Alerts + Notification channels; Domains + Traefik; v0.8b backups DB; v0.8a retention/purge.

## Recomendación única (siguiente)

**v0.8 Cron schedules** — jobs programados de producto (SYNC_HOST / pipeline / backup-adjacent) sobre el worker embebido ya existente.

## Por qué es el paso más rentable ahora

1. v0.7 access/edge/alerts queda cerrado a nivel mínimo.
2. Cron cierra el hueco de resilience v0.8 junto a backup+retention ya shipped.
3. Hardening docs / restore UI completa pueden seguir después sin bloquear schedules.

## Alcance concreto del incremento

1. Entidad `CronJob` (expr, target type, enabled) + migración + CRUD API ADMIN/OPERATOR.
2. Scheduler que encola jobs existentes según cron.
3. UI mínima `/cron`: listar/crear/enable.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka, no Cloudflare API real, no restore UI completa, no Teams globales.

## Definición de éxito

> OPERATOR puede crear un cron que encola SYNC_HOST o similar; deploy/webhooks/RBAC/alerts/backup intactos.
