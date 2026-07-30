# Siguiente paso de implementación

## Estado del último incremento (completado)

**Project DB TTL credentials (ADR-0015 opción C):**

- Emisión `POST /projects/{id}/database/credentials` — rol efímero `VALID UNTIL`, perfiles `db.read` (default) / `db.migrate` / `db.admin`.
- List + revoke; audit `PROJECT_DB_CREDENTIAL_*`; UI panel Database (issue URL + revoke).
- No rota `db.url` (migrator permanente intacto). SQL proxy (opción B) sigue diferido.

**Previo:** provisioner slice 1; envFrom secrets inject; índice `idx_projects_name_lower` + IT 5k; Billing/usage; PUBLIC minify + TLS; Host sync capabilities; OpenAPI + sunset `/applications`; UX Domains 403; Pipeline `hostId`; `migrateCommand`; Cloudflare scopes; Auto-deploy; Proxmox REUSED; DNS CNAME; Tunnel PUBLIC.

## Recomendación única (siguiente)

**Feature flags / plan local (enterprise flag)** — endurecer entitlements ya esbozados en billing. Alternativa: adapter Podman vía `RuntimeOrchestratorPort` solo si hay demanda.

## Por qué es el paso más rentable ahora

1. ADR-0015 (A+C) cerrado; SQL proxy B fuera de scope.
2. Soft limits / Stripe / Redis-Kafka siguen fuera.
3. Flags cierran envelope comercial v0.9 sin tocar runtime.

## Alcance concreto del incremento (siguiente)

1. Feature flags / plan local usable (enterprise flag + gating UI/API mínimo).
2. Tests + docs; sin Stripe obligatorio / sin Redis-Kafka / sin SQL console proxy.

## Secundario (si sobra capacidad)

- Adapter Podman vía `RuntimeOrchestratorPort` (opt-in).
- Más meters (job minutes, backup GB).

## Cola (no es el siguiente obligatorio)

1. Convención viva: secret `db.url` (+ `db.schema`); schema `app_<slug>`; envFrom → Compose ✅.
2. Provisioner CREATE ROLE/SCHEMA + grants + UI metadata ✅.
3. URLs/credenciales TTL (opción C) ✅.
4. Proxy+RLS (B) diferido.

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): fases B–D + capabilities DB + sync probe + OpenAPI + PUBLIC hardening + envFrom inject hechas; Compose sigue adapter default. Podman/K8s = adapters futuros.

## Qué no hacer

- No billing Stripe obligatorio / AI / marketplace, no Redis/Kafka obligatorio.
- No eliminar `composePath` de DB antes de migrar callers restantes.
- No `compose down -v` ni tocar `.env` en runbooks de deploy.
- No retirar `/applications` antes de Sunset 2027-08-01.
- No SQL console / proxy RLS sin demanda explícita.
- No interferir con fixes de login Reelpath en paralelo.
- No apuntar `ATLAS_APP_DB_URL` a la DB `atlas`.

## Definición de éxito (siguiente)

> Feature flag / plan local operable para gating enterprise; sin romper SSO/deploy/envFrom/DB provisioner/TTL credentials.
