# Siguiente paso de implementación

## Estado del último incremento (completado)

**`RuntimeOrchestratorPort` + Host `runtimeCapabilities` (ADR-0014 fase D / v0.8.11):**

- Deploy habla `RuntimeOrchestratorPort.apply` (no `composeUp` directo).
- Adapter Compose delega a `ContainerRuntimePort` (inspect/logs/restart intactos).
- Host API expone `runtimeCapabilities` (hoy `["compose"]`); prep para filtros placement futuros.
- Sin segundo runtime; sin eliminar `compose_path`.

**Previo:** `composePath` opcional (fase C); lectura `atlas.yml` (fase B); Cloudflare scopes; Auto-deploy; stale RUNNING; Proxmox REUSED; DNS CNAME; Tunnel PUBLIC.

## Recomendación única (siguiente)

**Autopilot por webhook sin host pin:** Pipeline `hostId` opcional → `RunPipeline` / auto-deploy resuelve placement en cada run (SHARED/ISOLATED). Alternativa: persistir Host capability tags en DB + filtro en placement; o UX Domains 403 scopes.

## Por qué es el paso más rentable ahora

1. Fase D ya desacopla el port nombrado Compose; pipelines siguen pinneando host en create.
2. Autopilot deploy omite host; webhooks aún no — gap producto cerrado.
3. Segundo runtime (Podman/K8s) aún no; tags DB pueden esperar.

## Alcance concreto del incremento (siguiente)

1. `Pipeline.hostId` nullable; create/update/enable-auto-deploy sin pin obligatorio.
2. `RunPipeline` → `AutopilotPlacementService.resolveHost` cuando `hostId` ausente.
3. Tests + docs; migración Flyway nullable; UI Pipeline advanced host opcional.

## Secundario (si sobra capacidad)

- UX Domains: mensaje explícito si Ensure falla por 403 (scopes insuficientes).
- OpenAPI / deprecations `/applications`.
- Persistir `runtime_capabilities` en DB (hoy derivado en dominio).

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): fases B–D hechas; siguiente motor = adapters adicionales / tags persistidos. Compose sigue adapter default.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka obligatorio.
- No eliminar `composePath` de DB antes de migrar callers restantes.
- No `compose down -v` ni tocar `.env` en runbooks de deploy.

## Definición de éxito (siguiente)

> Webhook / auto-deploy sin host pin → Autopilot placement por run; deploy compose vía `RuntimeOrchestratorPort` sigue verde.
