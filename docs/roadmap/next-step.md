# Siguiente paso de implementación

## Estado del último incremento (completado)

**OpenAPI publicado + deprecation `/applications` (v0.8.16):**

- Snapshot `docs/api/openapi.json` + guía `docs/api/openapi.md`.
- Contract test `OpenApiContractIntegrationTest` (paths + `applications` deprecated).
- Path claro: `docs/api/deprecations.md` (Sunset 2027-08-01); alias **no** retirado.
- `OpenApiConfig` documenta deprecation; README/conventions/endpoints actualizados.

**Previo:** UX Domains 403 scopes (v0.8.15); Host `runtimeCapabilities` DB + filtro placement (v0.8.14); Pipeline `hostId` opcional; `migrateCommand`; RuntimeOrchestratorPort; composePath opcional; atlas.yml; Cloudflare scopes UI; Auto-deploy; stale RUNNING; Proxmox REUSED; DNS CNAME; Tunnel PUBLIC.

## Recomendación única (siguiente)

**Sync Host que detecte capabilities reales (Docker/Podman)** y escriba `runtime_capabilities`. Alternativa: billing/usage meters (v0.9).

## Por qué es el paso más rentable ahora

1. OpenAPI + sunset path listos; sync capabilities habilita segundo runtime cuando haya demanda Podman/K8s.
2. Billing meters cierran envelope comercial (v0.9).

## Alcance concreto del incremento (siguiente)

1. Host sync: inspección Docker/Podman → actualizar `runtime_capabilities` sin romper placement compose.
2. Tests + docs; sin segundo runtime obligatorio.

## Secundario (si sobra capacidad)

- Performance pass / usage meters (v0.9).

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): fases B–D + pipeline sin pin + capabilities DB + UX scopes + OpenAPI hechas; siguiente motor = adapters adicionales + sync capabilities. Compose sigue adapter default.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka obligatorio.
- No eliminar `composePath` de DB antes de migrar callers restantes.
- No `compose down -v` ni tocar `.env` en runbooks de deploy.
- No retirar `/applications` antes de Sunset 2027-08-01.

## Definición de éxito (siguiente)

> Host sync escribe capabilities reales (compose/podman) sin romper placement SHARED compose.
