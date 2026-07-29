# Siguiente paso de implementación

## Estado del último incremento (completado)

**Host sync capabilities reales (v0.8.17):**

- Sync inspecciona Docker + Podman; escribe `runtime_capabilities` (`compose` / `podman`).
- Unreachable o probe vacío → **no** pisa tags existentes (placement compose seguro).
- `RuntimeCapabilityDetector` + tests use case; soft-probe en `RealHostConnectorAdapter`.

**Previo:** OpenAPI + sunset `/applications` (v0.8.16); UX Domains 403 scopes (v0.8.15); Host capabilities DB + filtro placement (v0.8.14); Pipeline `hostId` opcional; `migrateCommand`; RuntimeOrchestratorPort; composePath opcional; atlas.yml; Cloudflare scopes UI; Auto-deploy; stale RUNNING; Proxmox REUSED; DNS CNAME; Tunnel PUBLIC.

## Recomendación única (siguiente)

**Billing/usage meters (v0.9)** o performance pass sintético. Alternativa: adapter Podman real (solo si hay demanda).

## Por qué es el paso más rentable ahora

1. Capabilities sync listo; envelope comercial (meters/entitlements) desbloquea polish v0.9.
2. Segundo runtime adapter no urgente mientras Compose cubre flota.

## Alcance concreto del incremento (siguiente)

1. Usage meters + entitlements UI mínima (precio puede ser 0).
2. O: performance pass (5k projects synthetic).
3. Tests + docs; sin Stripe obligatorio.

## Secundario (si sobra capacidad)

- Adapter Podman vía `RuntimeOrchestratorPort` (opt-in).
- Feature flags / plan local.

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): fases B–D + capabilities DB + sync probe + OpenAPI hechas; Compose sigue adapter default. Podman/K8s = adapters futuros.

## Qué no hacer

- No billing Stripe obligatorio / AI / marketplace, no Redis/Kafka obligatorio.
- No eliminar `composePath` de DB antes de migrar callers restantes.
- No `compose down -v` ni tocar `.env` en runbooks de deploy.
- No retirar `/applications` antes de Sunset 2027-08-01.

## Definición de éxito (siguiente)

> Informe de usage exportable y/o carga objetivo validada (criterio v0.9); sin romper SSO/deploy compose.
