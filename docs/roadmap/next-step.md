# Siguiente paso de implementación

## Estado del último incremento (completado)

**ADR-0017 — Secretos de usuario para apps (re-enfoque):**

- Atlas = almacén + inject de secrets que el usuario crea para sus apps — **no** “Atlas ofrece AI”.
- UI: create/rotate project + org; delete org; copy explica `envFrom` → runtime.
- PUT upsert + DELETE `/secrets/{id}`; seed script genérico (extras libres).
- Docs: ADR-0017 + [secrets-for-apps.md](../product/secrets-for-apps.md); stub redirect en `platform-provided-ai.md`.

**Previo:** Secrets project + bindings (`33606f8`); envFrom inject; DB provisioner/TTL; feature flags; PUBLIC minify + TLS; Host sync; OpenAPI; Pipeline `hostId`; `migrateCommand`; Autopilot Tunnel/DNS/Proxmox.

## Recomendación única (siguiente)

**Adapter Podman** vía `RuntimeOrchestratorPort` (opt-in) — host sync ya anuncia `podman`; falta deploy path. Alternativa: slice Reelpath (prefer env Atlas sobre `PlatformSecret` in-app) cuando el repo esté a mano.

## Por qué es el paso más rentable ahora

1. Envelope comercial v0.9 cerrado (usage + plan + flags).
2. Flujo secrets→apps cerrado en Atlas (UI + API + envFrom); cutover app = trabajo aparte.
3. Capabilities Podman ya en hosts; adapter cierra gap ADR-0014 sin tocar Compose default.

## Alcance concreto del incremento (siguiente)

1. Adapter Podman opt-in vía `RuntimeOrchestratorPort` (deploy mínimo).
2. Tests + docs; sin Stripe / sin Redis-Kafka / sin SQL console proxy.

## Secundario (si sobra capacidad)

- Reelpath: prefer env Atlas → fallback PlatformSecret; `atlas.yml` envFrom con keys del usuario.
- Más meters (job minutes, backup GB).
- Hard-enforce soft limits (hoy solo reportan).

## Cola (no es el siguiente obligatorio)

1. Convención viva: secret `db.url` (+ `db.schema`); schema `app_<slug>`; envFrom → Compose ✅
2. Provisioner CREATE ROLE/SCHEMA + grants + UI metadata ✅
3. URLs/credenciales TTL (opción C) ✅
4. Feature flags / plan local ✅
5. Secrets usuario→app (UI rotate + docs ADR-0017) ✅; cutover Reelpath pendiente
6. Proxy+RLS (B) diferido

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): fases B–D + capabilities DB + sync probe + OpenAPI + PUBLIC hardening + envFrom inject hechas; Compose sigue adapter default. Podman/K8s = adapters futuros.

## Qué no hacer

- No billing Stripe obligatorio / marketplace, no Redis/Kafka obligatorio.
- No gateway LLM / metering tokens en Atlas sin demanda.
- No presentar Atlas como “proveedor de AI”; secrets son del usuario/ops para sus apps.
- No wipe `PlatformSecret` en DBs de apps en migración.
- No eliminar `composePath` de DB antes de migrar callers restantes.
- No `compose down -v` ni tocar `.env` en runbooks de deploy.
- No retirar `/applications` antes de Sunset 2027-08-01.
- No SQL console / proxy RLS sin demanda explícita.
- No interferir con fixes de login Reelpath en paralelo.
- No apuntar `ATLAS_APP_DB_URL` a la DB `atlas`.

## Definición de éxito (siguiente)

> Deploy vía adapter Podman en host con capability `podman` (opt-in); Compose default intacto; SSO/deploy/envFrom/DB provisioner/TTL/flags/secrets UI sin romper.
