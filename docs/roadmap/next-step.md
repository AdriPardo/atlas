# Siguiente paso de implementación

## Estado del último incremento (completado)

**v0.5 — Runtime visibility** ya está en el árbol:

- `ContainerRuntimePort` ampliado: `listContainers`, `containerLogs`, `restartContainer` (LOCAL + SSH vía adapters existentes).
- API: `GET /hosts/{id}/containers`, `GET .../logs`, `POST .../restart`.
- `GET /api/v1/settings/observability` + env `ATLAS_GRAFANA_BASE_URL` / `ATLAS_LOKI_BASE_URL` (deep-links Grafana Explore).
- UI Host detail: tabla de containers, LogViewer runtime, Restart, enlaces Loki/Grafana.
- Tests: use cases, parse contract `docker ps` JSON, adapter observability, smoke integración.

## Recomendación única (siguiente)

**v0.6 Pipelines mínimos** — definition + run + steps que encolan `DEPLOY_SERVICE` (GitOps ligero).

## Por qué es el paso más rentable ahora

1. **Runtime ya es diagnosticable** (containers/logs/links) — el siguiente dolor es automatizar “push → deploy” sin cron manual.
2. **Worker + `DEPLOY_SERVICE` ya existen** — un pipeline mínimo reutiliza la cola sin reescribir el runtime.
3. **Alcance acotado** — definición YAML/JSON simple, un trigger manual + webhook opcional después; no Redis/Kafka.
4. Roadmap v0.6 cierra el loop GitOps ligero antes de RBAC (v0.7).

## Alcance concreto del incremento

1. Tablas `pipelines` / `pipeline_runs` (Flyway) + dominio mínimo.
2. API CRUD pipeline + `POST /pipelines/{id}/run` → steps → job `DEPLOY_SERVICE`.
3. UI lista/detalle de pipelines y runs (estado + link a deployment/job).
4. Tests de use case + smoke API.

## Qué no hacer en este incremento

- No marketplace, no multi-tenant, no Redis/Kafka obligatorio.
- No RBAC completo (v0.7), no billing/AI.
- No reescribir el worker embebido ni el path compose real.

## Alternativa cercana

**v0.7 RBAC + Network** — solo si la demo clave es multi-usuario OPERATOR antes que automatización Git.

## Definición de éxito

> Un operador define un pipeline de deploy y lo ejecuta desde la UI; el run encola `DEPLOY_SERVICE` y aparece ligado a deployment/job; hosts/containers de v0.5 siguen funcionando.
