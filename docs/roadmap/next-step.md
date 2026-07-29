# Siguiente paso de implementación

## Estado del último incremento (completado)

**UX Domains 403 scopes (v0.8.15):**

- `CloudflareApiErrorMessages`: 403 Tunnel/DNS → mensaje `token scopes insufficient` (+ scopes + Org/Project secrets hint).
- Adapters Ensure `FAILED` con frase estable; tests unitarios.
- UI Domains: warning + link `/secrets` si scopes; Publish incompleto si Ensure falló.
- Docs networking + public-customer-hostname.

**Previo:** Host `runtimeCapabilities` DB + filtro placement (v0.8.14); Pipeline `hostId` opcional (v0.8.12); `migrateCommand` (v0.8.13); RuntimeOrchestratorPort; composePath opcional; atlas.yml; Cloudflare scopes UI; Auto-deploy; stale RUNNING; Proxmox REUSED; DNS CNAME; Tunnel PUBLIC.

## Recomendación única (siguiente)

**OpenAPI / deprecations `/applications`**, o sync Host que detecte capabilities reales (Docker/Podman). Alternativa: billing/usage meters (v0.9).

## Por qué es el paso más rentable ahora

1. Operador ya entiende 403 scopes; deuda API (OpenAPI + sunset `/applications`) bloquea v0.9 polish.
2. Sync capabilities habilita segundo runtime cuando haya demanda Podman/K8s.
3. Billing meters cierran envelope comercial.

## Alcance concreto del incremento (siguiente)

1. Publicar OpenAPI (o regenerar) + marcar/retirar alias `/applications` según sunset.
2. O: Host sync enriquece `runtime_capabilities` desde inspección runtime.
3. Tests + docs; sin segundo runtime obligatorio.

## Secundario (si sobra capacidad)

- Sync Host capabilities desde Docker/Podman probe.
- Performance pass / usage meters (v0.9).

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): fases B–D + pipeline sin pin + capabilities DB + UX scopes hechas; siguiente motor = adapters adicionales. Compose sigue adapter default.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka obligatorio.
- No eliminar `composePath` de DB antes de migrar callers restantes.
- No `compose down -v` ni tocar `.env` en runbooks de deploy.

## Definición de éxito (siguiente)

> OpenAPI usable por clientes externos y/o `/applications` deprecado con path claro; o Host sync escribe capabilities reales sin romper placement compose.
