# Siguiente paso de implementación

## Estado del último incremento (completado)

**v0.7 Alerts + Notification channels** ya está en el árbol:

- Entidades `AlertRule` + `NotificationChannel` (WEBHOOK/EMAIL) + migración Flyway `V13`.
- CRUD `GET/POST/PUT/DELETE /api/v1/alerts` y `/api/v1/notification-channels` (+ `POST /alerts/{id}/silence`), ACL ADMIN/OPERATOR.
- Evaluación best-effort en fallos de deploy (`DEPLOY_FAILED`) y job (`JOB_FAILED`) con entrega stub + audit `ALERT_FIRED`.
- UI `/alerts`: listar/crear canales y reglas.

**Previo:** Domains + Traefik metadata; v0.8b backups DB; v0.8a retention/purge; v0.6.1 Git webhooks; ACL project memberships.

## Recomendación única (siguiente)

**v0.7 remainder — Project roles VIEWER/DEVELOPER** — afinar ACL por project (hoy OPERATOR/ADMIN globales + memberships) sin abrir billing.

## Por qué es el paso más rentable ahora

1. Alerts cierra el bloque observabilidad mínima de v0.7.
2. Roles finos desbloquean multi-user serio del roadmap v0.7 sin Cloudflare/billing.
3. Cloudflare DNS sync real sigue aplazable (stub de domains ya cubre el control-plane).

## Alcance concreto del incremento

1. Extender `ProjectMemberRole` / permisos READ vs WRITE vs MANAGE para VIEWER y DEVELOPER.
2. Ajustar endpoints sensibles (deploy, secrets, alerts write) a la matriz.
3. UI memberships: seleccionar rol VIEWER|DEVELOPER|OPERATOR.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka, no Cloudflare API real aún, no restore UI completa, no outbound webhook HTTP real.

## Definición de éxito

> Un VIEWER puede leer un project pero no crear deploys; DEVELOPER escribe servicios/pipelines; OPERATOR/alerts/domains/backup intactos.
