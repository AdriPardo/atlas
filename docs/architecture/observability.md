# Arquitectura — Observabilidad

## Decisión

Atlas **no** sustituye Prometheus, Grafana ni Loki. La instalación ya los tiene; Atlas:

1. Expone sus propias métricas/logs (Micrometer → Prometheus, logs JSON → Loki).
2. Guarda **metadata** de paneles, deep-links y reglas de alerta orientadas a proyectos.
3. Ofrece en UI vistas “ops” que consultan Prometheus/Loki vía puertos (`MetricsQueryPort`, `LogsQueryPort`).

Ver [ADR-0007](../decisions/ADR-0007-observability-external-stack.md).

## Señales

| Señal | Origen Atlas | Destino |
|-------|--------------|---------|
| Métricas API/worker | Micrometer / Actuator | Prometheus scrape |
| Logs app | stdout JSON | Promtail/Fluent → Loki |
| Traces (fase posterior) | Micrometer Tracing / OTel | Tempo/Jaeger opcional |
| Health | `/actuator/health` | Traefik / Compose / uptime |

## Métricas de producto (diseño)

- `atlas_http_server_requests_*` (estándar Spring)
- `atlas_deployments_total{status=}`
- `atlas_jobs_pending` / `running` / `failed`
- `atlas_hosts_online`
- `atlas_projects_total`

## Logs de despliegue

- Durante el job: append a storage (DB text inicial → object/file o Loki labels `deployment_id=`).
- UI LogViewer: cola desde API o proxy Loki con ACL.

MVP actual guarda `deployments.logs` TEXT — válido hasta volumen; migrar a chunks o Loki en v0.5+.

## Alertas

| Capa | Responsable |
|------|-------------|
| Infra (disco, CPU host) | Alertmanager / Grafana on-call existente |
| Producto (deploy failed, host offline) | Módulo Alerts de Atlas → Notifications |

Reglas Atlas: expresan condición sobre métricas Prometheus o eventos internos; no duplicar Alertmanager completo.

## UX Observability

Sidebar grupo **Observability**: Logs, Metrics, Alerts. Cada Project tiene pestaña Logs/Metrics con deep-link a Grafana si el admin configuró `grafanaBaseUrl`.

## SLOs internos (orientativos)

| Servicio | SLO |
|----------|-----|
| API availability | 99.5% mensual (self-hosted, best-effort doc) |
| Deploy job start latency | p95 < 30s en cola vacía |
| UI TTFB dashboard | p95 < 1s (cache) |
