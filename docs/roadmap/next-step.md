# Siguiente paso de implementación

## Estado del último incremento (completado)

**Performance 5k + envFrom secrets inject (v0.9):**

- Deploy inyecta `envFrom.secretRef` (`runtime` + `services.*`) al `.env` del workspace (`db.url` → `DATABASE_URL`; override `env:`/`as:`). Sin filtrar valores en logs; secret ausente → warn+skip.
- Índice `idx_projects_name_lower`; IT `ProjectsScaleIntegrationTest` seed JDBC 5k + smoke list/search < 2s.

**Previo:** Billing/usage meters; PUBLIC minify + TLS (ADR-0016); Host sync capabilities; OpenAPI + sunset `/applications`; UX Domains 403; Host capabilities DB; Pipeline `hostId` opcional; `migrateCommand`; RuntimeOrchestratorPort; Cloudflare scopes; Auto-deploy; stale RUNNING; Proxmox REUSED; DNS CNAME; Tunnel PUBLIC.

**Docs (provisioner pendiente):** [ADR-0015](../decisions/ADR-0015-project-database-access.md) — roles+schemas; entrega `db.url` vía envFrom **operable**; nota [project-database-access.md](../product/project-database-access.md).

## Recomendación única (siguiente)

**Project DB access — slice 1 (provisioner)** ([ADR-0015](../decisions/ADR-0015-project-database-access.md)): CREATE ROLE/SCHEMA + grants `db.read` / `db.migrate`; UI metadata Project; **sin** SQL proxy. Alternativa: feature flags / plan local endurecido; adapter Podman solo si hay demanda.

## Por qué es el paso más rentable ahora

1. Envelope comercial + carga 5k + entrega secret→Compose listos; falta automatizar schema/rol Postgres.
2. Operador ya puede pegar `db.url` a mano; provisioner cierra el loop ADR-0015.
3. Soft limits / Stripe / Redis-Kafka siguen fuera de scope.

## Alcance concreto del incremento (siguiente)

1. Provisioner CREATE ROLE/SCHEMA + grants `db.read` / `db.migrate`; UI metadata Project; **sin** SQL proxy.
2. Persistir/rotar secret `db.url` tras provision.
3. Tests + docs; sin Stripe / sin Redis-Kafka obligatorio.

## Secundario (si sobra capacidad)

- Feature flags / plan local (enterprise flag).
- Adapter Podman vía `RuntimeOrchestratorPort` (opt-in).
- Más meters (job minutes, backup GB).

## Cola (no es el siguiente obligatorio)

1. Convención viva: secret `db.url` (+ `db.schema`); schema `app_<slug>`; envFrom → Compose ✅.
2. **Build siguiente:** provisioner CREATE ROLE/SCHEMA + grants; UI metadata; **sin** SQL proxy.
3. Luego: URLs/credenciales TTL (opción C). Proxy+RLS (B) diferido.

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): fases B–D + capabilities DB + sync probe + OpenAPI + PUBLIC hardening + envFrom inject hechas; Compose sigue adapter default. Podman/K8s = adapters futuros.

## Qué no hacer

- No billing Stripe obligatorio / AI / marketplace, no Redis/Kafka obligatorio.
- No eliminar `composePath` de DB antes de migrar callers restantes.
- No `compose down -v` ni tocar `.env` en runbooks de deploy.
- No retirar `/applications` antes de Sunset 2027-08-01.
- No SQL console / proxy RLS antes de cerrar provisioner slice 1.
- No interferir con fixes de login Reelpath en paralelo.

## Definición de éxito (siguiente)

> Provisioner DB slice 1 operable (rol+schema+grants + UI metadata); sin romper SSO/deploy compose / envFrom.
