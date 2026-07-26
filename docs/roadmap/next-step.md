# Siguiente paso de implementación

## Estado del último incremento (completado)

**v0.3 + v0.4 — Deploy real mínimo** ya está en el árbol:

- Tabla `jobs` + claim `FOR UPDATE SKIP LOCKED`, worker embebido (`atlas.worker.enabled`, mismo proceso API).
- Secrets cifrados AES (`ATLAS_SECRETS_MASTER_KEY`) + campos SSH en Host (`LOCAL|SSH`).
- `SYNC_HOST` y `DEPLOY_SERVICE` con adapters reales (JGit, docker compose local/SSH); `Unsupported*` solo si `atlas.adapters.real-enabled=false`.
- UI: Deploy (elige host) → detalle con poll de logs; Sync en host.

## Recomendación única (siguiente)

**Migración Application → Project + Service (v0.2 formal del roadmap), con alias de API `/applications` durante la transición.**

## Por qué es el paso más rentable ahora

1. **El camino de deploy ya existe** — seguir construyendo pipelines, RBAC y observability encima del vocabulario `Application` encarece el rename (ADR-0004).
2. **Alineación comercial** — el operador y la doc hablan de Projects/Services; la UI/API aún no.
3. **Observabilidad (v0.5) es el siguiente valor de producto**, pero diagnosticar flota sin el modelo mental Project/Service genera deuda de naming en listados, ACL y pipelines.
4. **Alcance acotado** — migración Flyway + alias REST + redirects UI; no toca el worker ni Authentik.

## Alcance concreto del incremento

1. Tablas `projects` / `services` (o rename controlado) + seed Organization (1 fila).
2. API `/api/v1/projects`, `/api/v1/services`; mantener `/applications` como alias deprecado.
3. UI rename + redirects desde rutas `/applications`.
4. Jobs/Deployments referencian Service (o mantienen FK con vista de compatibilidad).
5. Tests de migración + contrato OpenAPI.

## Qué no hacer en este incremento

- No marketplace, no multi-tenant, no Redis/Kafka.
- No reescribir el worker ni los adapters de deploy.
- No Billing / AI.

## Alternativa cercana (si se prioriza demo ops)

**v0.5 Runtime visibility** (containers por host, logs, deep-links Grafana/Loki) — elegirla solo si el rename Project puede esperar 1–2 sprints y la prioridad es diagnosticar deploys fallidos sin SSH.

## Definición de éxito

> El operador trabaja solo en “Projects/Services”; deploys existentes siguen funcionando; `/applications` responde con deprecation clara.
