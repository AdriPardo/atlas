# Siguiente paso de implementación

## Estado del último incremento (completado)

**Pipeline `hostId` opcional + Autopilot en webhook/run (v0.8.12):**

- `Pipeline.hostId` nullable (Flyway V19); create/update/enable-auto-deploy sin pin obligatorio.
- `enable-auto-deploy` guarda pipeline sin host por defecto (Reelpath-safe: SHARED vía Autopilot en cada push).
- `RunPipeline` / webhook pasan `hostId` null → `DeployServiceUseCase` → `AutopilotPlacementService.resolveHost` (SHARED; ISOLATED si el deploy lo pide).
- UI Pipeline: host en Advanced opcional; detalle/lista muestran “Autopilot”.

**Previo:** `RuntimeOrchestratorPort` + Host `runtimeCapabilities` (fase D); `composePath` opcional (fase C); lectura `atlas.yml` (fase B); Cloudflare scopes; Auto-deploy; stale RUNNING; Proxmox REUSED; DNS CNAME; Tunnel PUBLIC.

## Recomendación única (siguiente)

**Persistir Host `runtimeCapabilities` en DB + filtro en placement**, o UX Domains 403 scopes (mensaje explícito si Ensure falla por token insuficiente). Alternativa: OpenAPI / deprecations `/applications`.

## Por qué es el paso más rentable ahora

1. Webhook ya no pinnea host; placement puede filtrar por capability cuando tags existan en DB.
2. Domains 403 es dolor operador real y acotado.
3. Segundo runtime (Podman/K8s) aún no; adapters adicionales esperan tags.

## Alcance concreto del incremento (siguiente)

1. Columna / sync `runtime_capabilities` en Host (hoy derivado en dominio).
2. Placement SHARED filtra hosts que anuncien `compose` (o capability pedida).
3. Tests + docs; sin segundo runtime aún.

## Secundario (si sobra capacidad)

- UX Domains: mensaje explícito si Ensure falla por 403 (scopes insuficientes).
- OpenAPI / deprecations `/applications`.

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): fases B–D + pipeline sin pin hechas; siguiente motor = adapters adicionales / tags persistidos. Compose sigue adapter default.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka obligatorio.
- No eliminar `composePath` de DB antes de migrar callers restantes.
- No `compose down -v` ni tocar `.env` en runbooks de deploy.

## Definición de éxito (siguiente)

> Placement elige solo hosts con capability compatible; Host API/DB alineados; deploy compose sigue verde.
