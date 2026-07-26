# Siguiente paso de implementación

## Estado del último incremento (completado)

**v0.2 — Application → Project + Service** ya está en el árbol:

- Tablas `organizations` (seed 1 fila), `projects`, `services`; Flyway V8 migra cada `Application` → Project (mismo id) + Service `default`.
- `deployments.service_id` sustituye `application_id`.
- API `/api/v1/projects`, `/api/v1/services` (+ nested `/projects/{id}/services`, deploy en project/service).
- `/api/v1/applications` permanece como alias deprecado (headers `Deprecation` / `Sunset` / `Link`).
- UI: rutas `/projects`, redirects desde `/applications`; sidebar **Projects**.
- ACL: `ApplicationRepositoryAdapter` compone Project + default Service; worker/deploy sin rewrite.

## Recomendación única (siguiente)

**v0.5 Runtime visibility** — containers por host, logs de runtime y deep-links a Grafana/Loki.

## Por qué es el paso más rentable ahora

1. **Deploy path ya es real** (v0.3/v0.4) y el vocabulario Project/Service ya está alineado — el siguiente dolor de operador es diagnosticar deploys fallidos sin SSH.
2. **Pipelines (v0.6)** aportan orquestación, pero sin visibilidad de runtime el feedback loop sigue siendo opaco; observability desbloquea demos ops y reduce MTTR.
3. **Alcance acotado** — proyección `ContainerSnapshot` / list containers vía adapters existentes + UI en Host detail; no requiere Redis/Kafka ni multi-tenant.
4. **ADR-0007** ya apunta al stack externo de observability; Atlas solo necesita superficie de producto (list/logs/links).

## Alcance concreto del incremento

1. `GET /hosts/{id}/containers` (+ restart opcional si seguro).
2. UI Host detail: lista containers / estado / deep-link logs.
3. Config de deep-links Grafana/Loki (settings o env).
4. Tests de contrato del port de runtime + smoke UI.

## Qué no hacer en este incremento

- No marketplace, no multi-tenant, no Redis/Kafka.
- No reescribir el worker embebido.
- No Billing / AI / pipelines completos.

## Alternativa cercana (si se prioriza delivery automation)

**v0.6 Pipelines mínimos** (definition + run + steps encolando `DEPLOY_SERVICE`) — elegirla solo si la demo clave es “push → deploy” y la flota ya se diagnostica bien por SSH/logs de job.

## Definición de éxito

> El operador ve containers y enlaces de logs por host desde la UI; deploys Project/Service siguen funcionando; `/applications` sigue deprecado sin romper clientes.
