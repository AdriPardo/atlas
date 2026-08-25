# Módulos — Observability de producto

## Logs

- Deploy logs: stream durante job; histórico por deployment.
- App logs: query Loki filtrado por labels (`project`, `service`, `host`) si está configurado.
- ACL: usuario solo ve logs de projects permitidos (post-RBAC).

## Metrics

- Resumen por Service (CPU/mem si cadvisor/node exporter labels).
- Embeds/deep-links Grafana (`grafanaBaseUrl` + dashboard UID templates).
- Atlas no almacena time-series propias salvo agregados cacheados cortos.

## Alerts

- Reglas: “deploy failed”, “host offline > N min”, umbrales PromQL guardados como texto.
- Estado: OK / PENDING / FIRING / SILENCED.
- Routing a Notifications.

## Notifications

Canales: email SMTP (ADR-0018, implementado), Slack webhook, generic webhook, (futuro) Discord/Teams.

Preferencias por usuario + overrides por Project. Throttling para evitar storms.
