# Siguiente paso de implementación

## Estado del último incremento (completado)

**v0.7 remainder — Domains + Traefik metadata** ya está en el árbol:

- Entidad `Domain` (project/service, estados `PENDING_DNS|ACTIVE|ERROR`) + cert metadata.
- CRUD anidado `GET/POST /api/v1/projects/{id}/domains` y `GET/PUT/DELETE /api/v1/domains/{id}`.
- `POST /domains/{id}/verify` (stub control-plane + TXT challenge) y labels Traefik (`GET .../traefik`, alias `/traefik/routes/{id}`).
- DNS provider stub (Cloudflare sync pendiente); UI en project detail (`ProjectDomainsPanel`).

**Previo:** v0.8b backups DB; v0.8a retention/purge; v0.6.1 Git webhooks; ACL project memberships.

## Recomendación única (siguiente)

**v0.7 Alerts + Notification channels** — reglas producto + destinos (email/webhook/Slack) sobre el edge/ACL ya cerrado.

## Por qué es el paso más rentable ahora

1. Domains/Traefik metadata cierra el resto networking de v0.7 mínimo.
2. Alerts desbloquean observabilidad operativa sin tocar billing/AI.
3. Teams globales siguen opcionales (ACL por project ya cubre OPERATOR).

## Alcance concreto del incremento

1. Entidades AlertRule + NotificationChannel + API CRUD (ADMIN/OPERATOR).
2. Evaluación mínima o stub de entrega (webhook) + audit.
3. UI mínima: listar/crear alerta y canal.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka, no Cloudflare API real aún, no restore UI completa.

## Definición de éxito

> OPERATOR puede crear una alerta y un canal; deploy/webhooks/RBAC/domains/backup intactos.
