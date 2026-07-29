# Siguiente paso de implementación

## Estado del último incremento (completado)

**Host `runtimeCapabilities` persistidos + filtro placement SHARED (v0.8.14):**

- Flyway V20: columna `hosts.runtime_capabilities` JSONB (default `["compose"]`).
- Domain `Host` guarda tags; create default `compose`; `replaceRuntimeCapabilities` para sync futuro.
- `AutopilotPlacementService` SHARED filtra hosts con `supportsRuntime(COMPOSE)`; si ninguno → seed `atlas-local`.
- API response sin cambio de contrato (tags desde DB). Tests placement + domain.

**Previo:** Pipeline `hostId` opcional + Autopilot webhook (v0.8.12); `migrateCommand` (v0.8.13); `RuntimeOrchestratorPort` (fase D); `composePath` opcional (fase C); lectura `atlas.yml` (fase B); Cloudflare scopes; Auto-deploy; stale RUNNING; Proxmox REUSED; DNS CNAME; Tunnel PUBLIC.

## Recomendación única (siguiente)

**UX Domains 403 scopes** (mensaje explícito si Ensure falla por token insuficiente), o OpenAPI / deprecations `/applications`. Alternativa: sync Host que detecte capabilities reales (Docker/Podman).

## Por qué es el paso más rentable ahora

1. Placement ya filtra por capability; operador aún tropieza con Cloudflare 403 opaco.
2. OpenAPI/deprecations cierran deuda API antes de v0.9.
3. Segundo runtime (Podman/K8s) aún no; adapters adicionales esperan demanda.

## Alcance concreto del incremento (siguiente)

1. Domains Ensure: mapear 403 Cloudflare → mensaje “token scopes insuficientes” (+ link a scopes UI).
2. Tests + docs; sin segundo runtime.

## Secundario (si sobra capacidad)

- OpenAPI / deprecations `/applications`.
- Sync Host: enriquecer `runtime_capabilities` desde inspección runtime.

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): fases B–D + pipeline sin pin + capabilities DB hechas; siguiente motor = adapters adicionales. Compose sigue adapter default.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka obligatorio.
- No eliminar `composePath` de DB antes de migrar callers restantes.
- No `compose down -v` ni tocar `.env` en runbooks de deploy.

## Definición de éxito (siguiente)

> Ensure Domain falla por scopes → UI/API dice scopes faltantes; create Tunnel/DNS sigue verde con token correcto.
