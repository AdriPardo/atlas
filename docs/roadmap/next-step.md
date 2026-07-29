# Siguiente paso de implementación

## Estado del último incremento (completado)

**Billing/usage meters (v0.9 slice):**

- Tabla `usage_records` (Flyway V21); domain `UsageRecord` + `PlanEntitlement`.
- `BillingMeterPort` in-process; `deploy.count` al encolar deploy.
- API `GET /billing/usage` + `GET /billing/entitlements` (ADMIN); UI `/billing` + export CSV.
- Plan local `community` precio 0; soft limits; sin Stripe.

**Previo:** PUBLIC minify + TLS (v0.8.18 / ADR-0016); Host sync capabilities (v0.8.17); OpenAPI + sunset `/applications` (v0.8.16); UX Domains 403 scopes; Host capabilities DB; Pipeline `hostId` opcional; `migrateCommand`; RuntimeOrchestratorPort; Cloudflare scopes; Auto-deploy; stale RUNNING; Proxmox REUSED; DNS CNAME; Tunnel PUBLIC.

**Docs (sin código de provisioning):** [ADR-0015](../decisions/ADR-0015-project-database-access.md) — acceso DB por Project (roles+schemas; secrets `db.url`); nota [project-database-access.md](../product/project-database-access.md).

## Recomendación única (siguiente)

**Performance pass sintético (5k projects)** o **Project DB access — slice 1 (provisioner)** ([ADR-0015](../decisions/ADR-0015-project-database-access.md)). Alternativa: feature flags / plan local endurecido; adapter Podman solo si hay demanda.

## Por qué es el paso más rentable ahora

1. Envelope comercial (meters/entitlements) listo; falta validar carga v0.9 o desbloquear DB apps.
2. Soft limits no bloquean deploy; performance pass cierra criterio “carga objetivo”.
3. Provisioner Postgres es incremento propio documentado; ya no bloqueado por billing.

## Alcance concreto del incremento (siguiente)

1. Performance: seed sintético ~5k projects + smoke list/search (criterio v0.9).
2. O: provisioner CREATE ROLE/SCHEMA + grants `db.read` / `db.migrate`; UI metadata Project; **sin** SQL proxy.
3. Tests + docs; sin Stripe / sin Redis-Kafka obligatorio.

## Secundario (si sobra capacidad)

- Feature flags / plan local (enterprise flag).
- Adapter Podman vía `RuntimeOrchestratorPort` (opt-in).
- Más meters (job minutes, backup GB).

## Cola (no es el siguiente obligatorio)

**Project DB access — slice 1** si se elige performance primero, o al revés:

1. Convención ya viva: secret `db.url` (+ `db.schema`) por project; schema `app_<slug>`.
2. Build: provisioner CREATE ROLE/SCHEMA + grants; UI metadata; **sin** SQL proxy.
3. Luego: URLs/credenciales TTL (opción C). Proxy+RLS (B) diferido.

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): fases B–D + capabilities DB + sync probe + OpenAPI + PUBLIC hardening hechas; Compose sigue adapter default. Podman/K8s = adapters futuros.

## Qué no hacer

- No billing Stripe obligatorio / AI / marketplace, no Redis/Kafka obligatorio.
- No eliminar `composePath` de DB antes de migrar callers restantes.
- No `compose down -v` ni tocar `.env` en runbooks de deploy.
- No retirar `/applications` antes de Sunset 2027-08-01.
- No SQL console / proxy RLS antes de cerrar provisioner slice 1.
- No interferir con fixes de login Reelpath en paralelo.

## Definición de éxito (siguiente)

> Carga objetivo validada (5k) **o** provisioner DB slice 1 operable; sin romper SSO/deploy compose.
