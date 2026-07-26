# Siguiente paso de implementación

## Estado del último incremento (completado)

**v0.7 — RBAC mínimo + Audit** ya está en el árbol:

- Flyway V10: `project_memberships` (VIEWER|DEVELOPER|OPERATOR) + `audit_entries`.
- `ProjectAuthorizationService`: ADMIN bypass; resto requiere membership + permiso (READ/WRITE/DEPLOY/MANAGE_MEMBERS).
- Crear project otorga membership OPERATOR al actor; list/get/update/delete/deploy/pipeline respetan ACL.
- API: `/projects/{id}/memberships`, `/audit` (ADMIN), `/users` (ADMIN).
- UI: Members en Project detail, página Audit, nav Audit.
- Tests: autorización unit + smoke forbid OPERATOR sin membership + audit tras pipeline run.

## Recomendación única (siguiente)

**v0.6.1 / v0.7b Git webhooks** — `POST /webhooks/git/{token}` dispara pipeline run (cierra GitOps ligero).

## Por qué es el paso más rentable ahora

1. Pipelines + RBAC ya existen; falta el trigger automático push→deploy del roadmap v0.6.
2. Alcance pequeño encima de `RunPipelineUseCase`.
3. Network/domains (resto v0.7) puede esperar tras demo GitOps.

## Alcance concreto del incremento

1. Tabla webhook tokens por project/pipeline.
2. Endpoint público autenticado por token → `RunPipelineUseCase`.
3. UI: mostrar/regenerar token en pipeline detail.
4. Tests de contrato webhook.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka, no Traefik domains completos aún.

## Definición de éxito

> Push (o curl webhook) encola el mismo pipeline run que “Run now”; ACL de projects intacta; audit registra el run.
