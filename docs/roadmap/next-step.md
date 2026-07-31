# Siguiente paso de implementación

## Estado del último incremento (completado)

**ADR-0017 — AI de plataforma (contrato):**

- AI = capacidad ops Atlas; **no** BYOK end-user en apps (Reelpath).
- Secrets lógicos `ai.openai` / `ai.elevenlabs` / `ai.deepseek` (+ genéricos `ai.provider` / `ai.api_key` / `ai.base_url`).
- envFrom mapeos + hints UI Secrets; nota producto + ejemplo `atlas.yml`.
- Migración Reelpath: repo app (no en este workspace) — env gana sobre PlatformSecret; UI keys detrás de flag; **no** wipe DB.

**Previo:** Feature flags / plan local; Project DB TTL (ADR-0015 C); provisioner; envFrom; Billing/usage; PUBLIC minify + TLS; Host sync; OpenAPI; Pipeline `hostId`; `migrateCommand`; Autopilot Tunnel/DNS/Proxmox.

## Recomendación única (siguiente)

**Adapter Podman** vía `RuntimeOrchestratorPort` (opt-in) — host sync ya anuncia `podman`; falta deploy path. Alternativa: slice Reelpath (env-first + hide PlatformSecret AI UI) cuando el repo esté a mano.

## Por qué es el paso más rentable ahora

1. Envelope comercial v0.9 cerrado (usage + plan + flags).
2. Contrato AI cerrado en docs/mapeos; cutover app = trabajo aparte.
3. Capabilities Podman ya en hosts; adapter cierra gap ADR-0014 sin tocar Compose default.

## Alcance concreto del incremento (siguiente)

1. Adapter Podman opt-in vía `RuntimeOrchestratorPort` (deploy mínimo).
2. Tests + docs; sin Stripe / sin Redis-Kafka / sin SQL console proxy.

## Secundario (si sobra capacidad)

- Reelpath: prefer env Atlas → fallback PlatformSecret; flag hide UI keys; `atlas.yml` envFrom AI.
- Más meters (job minutes, backup GB).
- Hard-enforce soft limits (hoy solo reportan).

## Cola (no es el siguiente obligatorio)

1. Convención viva: secret `db.url` (+ `db.schema`); schema `app_<slug>`; envFrom → Compose ✅.
2. Provisioner CREATE ROLE/SCHEMA + grants + UI metadata ✅.
3. URLs/credenciales TTL (opción C) ✅.
4. Feature flags / plan local ✅.
5. AI plataforma contrato + envFrom mapeos ✅; cutover Reelpath pendiente.
6. Proxy+RLS (B) diferido.

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): fases B–D + capabilities DB + sync probe + OpenAPI + PUBLIC hardening + envFrom inject hechas; Compose sigue adapter default. Podman/K8s = adapters futuros. AI provider swap vía secrets ([ADR-0017](../decisions/ADR-0017-platform-provided-ai.md)) sin UI end-user.

## Qué no hacer

- No billing Stripe obligatorio / marketplace, no Redis/Kafka obligatorio.
- No gateway LLM / metering tokens en Atlas sin demanda.
- No pedir OpenAI/ElevenLabs keys al end-user en apps Atlas.
- No wipe `PlatformSecret` AI en DBs de apps en migración.
- No eliminar `composePath` de DB antes de migrar callers restantes.
- No `compose down -v` ni tocar `.env` en runbooks de deploy.
- No retirar `/applications` antes de Sunset 2027-08-01.
- No SQL console / proxy RLS sin demanda explícita.
- No interferir con fixes de login Reelpath en paralelo.
- No apuntar `ATLAS_APP_DB_URL` a la DB `atlas`.

## Definición de éxito (siguiente)

> Deploy vía adapter Podman en host con capability `podman` (opt-in); Compose default intacto; SSO/deploy/envFrom/DB provisioner/TTL/flags/AI-secret mappings sin romper.
