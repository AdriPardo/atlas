# Siguiente paso de implementación

## Estado del último incremento (completado)

**Feature flags / plan local (enterprise):**

- `ATLAS_PLAN_CODE=community|enterprise` + flags `billing` / `audit_export` (derive + override).
- `GET /settings/features`; gating billing API/UI; `GET /audit/export` (ADMIN + enterprise).
- Entitlements enterprise = límites unlimited (soft); UI nav + Export JSON audit.

**Previo:** Project DB TTL credentials (ADR-0015 C); provisioner slice 1; envFrom; índice 5k; Billing/usage; PUBLIC minify + TLS; Host sync capabilities; OpenAPI + sunset `/applications`; UX Domains 403; Pipeline `hostId`; `migrateCommand`; Cloudflare scopes; Auto-deploy; Proxmox REUSED; DNS CNAME; Tunnel PUBLIC.

## Recomendación única (siguiente)

**Adapter Podman** vía `RuntimeOrchestratorPort` (opt-in) — host sync ya anuncia `podman`; falta deploy path. Alternativa: más meters (job minutes, backup GB) si billing pide densificar informe.

## Por qué es el paso más rentable ahora

1. Envelope comercial v0.9 cerrado (usage + plan + flags).
2. Soft limits / Stripe / Redis-Kafka siguen fuera.
3. Capabilities Podman ya en hosts; adapter cierra gap ADR-0014 sin tocar Compose default.

## Alcance concreto del incremento (siguiente)

1. Adapter Podman opt-in vía `RuntimeOrchestratorPort` (deploy mínimo).
2. Tests + docs; sin Stripe / sin Redis-Kafka / sin SQL console proxy.

## Secundario (si sobra capacidad)

- Más meters (job minutes, backup GB).
- Hard-enforce soft limits (hoy solo reportan).

## Cola (no es el siguiente obligatorio)

1. Convención viva: secret `db.url` (+ `db.schema`); schema `app_<slug>`; envFrom → Compose ✅.
2. Provisioner CREATE ROLE/SCHEMA + grants + UI metadata ✅.
3. URLs/credenciales TTL (opción C) ✅.
4. Feature flags / plan local ✅.
5. Proxy+RLS (B) diferido.

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

> Deploy vía adapter Podman en host con capability `podman` (opt-in); Compose default intacto; SSO/deploy/envFrom/DB provisioner/TTL/flags sin romper.
