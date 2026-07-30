# Siguiente paso de implementación

## Estado del último incremento (completado)

**Project DB provisioner slice 1 (ADR-0015):**

- `ProjectDatabaseProvisionerPort` + Postgres adapter: `CREATE ROLE` / `CREATE SCHEMA` + grants `db.migrate` en DB dedicada `apps` (nunca control-plane `atlas`).
- API `GET/POST /projects/{id}/database[/provision]`; secrets `db.url` + `db.schema`; UI panel Database en Project detail.
- Config `ATLAS_APP_DB_URL` / `USERNAME` / `PASSWORD`; docker init `CREATE DATABASE apps`.

**Previo:** envFrom secrets inject; índice `idx_projects_name_lower` + IT 5k; Billing/usage; PUBLIC minify + TLS; Host sync capabilities; OpenAPI + sunset `/applications`; UX Domains 403; Pipeline `hostId`; `migrateCommand`; Cloudflare scopes; Auto-deploy; Proxmox REUSED; DNS CNAME; Tunnel PUBLIC.

## Recomendación única (siguiente)

**Credenciales TTL / URLs de corta vida (opción C sobre A)** ([ADR-0015](../decisions/ADR-0015-project-database-access.md)): emitir `db.read` temporal para consola local. Alternativa: feature flags / plan local; adapter Podman solo si hay demanda.

## Por qué es el paso más rentable ahora

1. Provisioner cierra aislamiento schema/rol; falta UX “click → connect” sin pegar password eterna.
2. Soft limits / Stripe / Redis-Kafka siguen fuera de scope.
3. SQL proxy+RLS (opción B) sigue diferido.

## Alcance concreto del incremento (siguiente)

1. Emitir credenciales / URLs TTL (`db.read` default humano; `db.migrate` ya es el rol app).
2. Tests + docs; sin Stripe / sin Redis-Kafka obligatorio / sin SQL console proxy.

## Secundario (si sobra capacidad)

- Feature flags / plan local (enterprise flag).
- Adapter Podman vía `RuntimeOrchestratorPort` (opt-in).
- Más meters (job minutes, backup GB).

## Cola (no es el siguiente obligatorio)

1. Convención viva: secret `db.url` (+ `db.schema`); schema `app_<slug>`; envFrom → Compose ✅.
2. Provisioner CREATE ROLE/SCHEMA + grants + UI metadata ✅.
3. **Build siguiente:** URLs/credenciales TTL (opción C).
4. Proxy+RLS (B) diferido.

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): fases B–D + capabilities DB + sync probe + OpenAPI + PUBLIC hardening + envFrom inject hechas; Compose sigue adapter default. Podman/K8s = adapters futuros.

## Qué no hacer

- No billing Stripe obligatorio / AI / marketplace, no Redis/Kafka obligatorio.
- No eliminar `composePath` de DB antes de migrar callers restantes.
- No `compose down -v` ni tocar `.env` en runbooks de deploy.
- No retirar `/applications` antes de Sunset 2027-08-01.
- No SQL console / proxy RLS antes de TTL credentials.
- No interferir con fixes de login Reelpath en paralelo.
- No apuntar `ATLAS_APP_DB_URL` a la DB `atlas`.

## Definición de éxito (siguiente)

> Emisión de credenciales TTL operable (read vs migrate) sobre roles provisionados; sin romper SSO/deploy/envFrom/provisioner.
