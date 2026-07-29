# Siguiente paso de implementación

## Estado del último incremento (completado)

**Host sync capabilities reales (v0.8.17):**

- Sync inspecciona Docker + Podman; escribe `runtime_capabilities` (`compose` / `podman`).
- Unreachable o probe vacío → **no** pisa tags existentes (placement compose seguro).
- `RuntimeCapabilityDetector` + tests use case; soft-probe en `RealHostConnectorAdapter`.

**Previo:** OpenAPI + sunset `/applications` (v0.8.16); UX Domains 403 scopes (v0.8.15); Host capabilities DB + filtro placement (v0.8.14); Pipeline `hostId` opcional; `migrateCommand`; RuntimeOrchestratorPort; composePath opcional; atlas.yml; Cloudflare scopes UI; Auto-deploy; stale RUNNING; Proxmox REUSED; DNS CNAME; Tunnel PUBLIC.

**Docs (sin código de provisioning):** [ADR-0015](../decisions/ADR-0015-project-database-access.md) — acceso DB por Project (roles+schemas; secrets `db.url`); nota [project-database-access.md](../product/project-database-access.md).

## Recomendación única (siguiente)

**Billing/usage meters (v0.9)** o performance pass sintético. Alternativa: adapter Podman real (solo si hay demanda).

## Por qué es el paso más rentable ahora

1. Capabilities sync listo; envelope comercial (meters/entitlements) desbloquea polish v0.9.
2. Segundo runtime adapter no urgente mientras Compose cubre flota.
3. Acceso DB de apps (ADR-0015) queda **encolado después de billing**: contrato + convención de secrets ya documentados; provisioner Postgres es incremento propio.

## Alcance concreto del incremento (siguiente)

1. Usage meters + entitlements UI mínima (precio puede ser 0).
2. O: performance pass (5k projects synthetic).
3. Tests + docs; sin Stripe obligatorio.

## Secundario (si sobra capacidad)

- Adapter Podman vía `RuntimeOrchestratorPort` (opt-in).
- Feature flags / plan local.

## Cola post-billing (no es el siguiente)

**Project DB access — slice 1 (provisioner)** ([ADR-0015](../decisions/ADR-0015-project-database-access.md)):

1. Convención ya viva: secret `db.url` (+ `db.schema`) por project; schema `app_<slug>`.
2. Build: provisioner CREATE ROLE/SCHEMA + grants `db.read` / `db.migrate`; UI metadata en Project; **sin** SQL proxy.
3. Luego: URLs/credenciales TTL (opción C). Proxy+RLS (B) diferido.

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): fases B–D + capabilities DB + sync probe + OpenAPI hechas; Compose sigue adapter default. Podman/K8s = adapters futuros.

## Qué no hacer

- No billing Stripe obligatorio / AI / marketplace, no Redis/Kafka obligatorio.
- No eliminar `composePath` de DB antes de migrar callers restantes.
- No `compose down -v` ni tocar `.env` en runbooks de deploy.
- No retirar `/applications` antes de Sunset 2027-08-01.
- No provisionar roles Postgres ni SQL console antes de cerrar v0.9 billing/usage (salvo urgencia ops documentada).
- No interferir con fixes de login Reelpath en paralelo.

## Definición de éxito (siguiente)

> Informe de usage exportable y/o carga objetivo validada (criterio v0.9); sin romper SSO/deploy compose.
