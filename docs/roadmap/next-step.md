# Siguiente paso de implementación

## Estado del último incremento (completado)

**v0.6 — Pipelines mínimos** ya está en el árbol:

- Flyway V9: `pipelines` + `pipeline_runs` (service_id + host_id = definición deploy-centric).
- API CRUD `/api/v1/pipelines` + `POST /pipelines/{id}/runs` (202) → reutiliza `DeployServiceUseCase` / job `DEPLOY_SERVICE`.
- Runs sincronizan status ligero desde deployment al listar/consultar.
- UI: `/pipelines` lista/crear/detalle con Run now + links a deployment.
- Tests: `RunPipelineUseCaseTest` + smoke integración create/run.

## Recomendación única (siguiente)

**v0.7 Access & edge (RBAC mínimo)** — Teams/Permissions VIEWER|DEVELOPER|OPERATOR + audit log básico.

## Por qué es el paso más rentable ahora

1. **Catalog + deploy + runtime + pipelines** ya cubren el loop ops single-admin; el siguiente dolor real es multi-usuario seguro.
2. Authentik SSO ya clasifica ADMIN vs OPERATOR — falta ACL por project y audit trail.
3. Network/domains pueden ir en un micro-incremento posterior si RBAC es el bloqueador comercial.

## Alcance concreto del incremento

1. Modelo permissions (project membership) + enforcement en use cases/API.
2. Audit log append-only en mutaciones clave (deploy, pipeline run, secret).
3. UI: miembros de project (mínimo) o listado audit.
4. Tests de denegación OPERATOR sin membership.

## Qué no hacer en este incremento

- No marketplace, billing, AI, Redis/Kafka.
- No Traefik/certificates completos (puede ser follow-up v0.7b).
- No webhooks Git todavía (seguirán a RBAC o como v0.6.1).

## Alternativa cercana

**v0.6.1 Git webhooks** — si la demo clave es push→deploy antes que multi-user.

## Definición de éxito

> OPERATOR sin membership no despliega projects ajenos; ADMIN sigue full-access; pipeline/runtime de v0.5–v0.6 intactos; audit registra al menos deploy/run.
