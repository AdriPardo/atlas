# Siguiente paso de implementación

## Estado del último incremento (completado)

**Reelpath cutover secrets (env Atlas → PlatformSecret fallback):**

- Runtime: `pickFirstSecret` — env (Atlas envFrom) → PlatformSecret → leftover org BYOK.
- UI end-user: sin paste BYOK; solo estado. Forms keys → Admin → Infra (`PLATFORM_ADMIN_EMAILS`).
- `atlas.yml` envFrom: `AUTH_SECRET`, `CREDENTIALS_ENCRYPTION_KEY`, `ai.openai` / `ai.elevenlabs` / `ai.deepseek`.
- Sin wipe DB / filas PlatformSecret.

**Previo:** Adapter Podman opt-in (ADR-0014); ADR-0017 secrets usuario→apps; Secrets project + bindings; envFrom inject; DB provisioner/TTL; feature flags; PUBLIC minify + TLS; Host sync; OpenAPI; Pipeline `hostId`; `migrateCommand`; Autopilot Tunnel/DNS/Proxmox.

## Recomendación única (siguiente)

**Autopilot SHARED placement por capability pedida** — hoy SHARED filtra solo `compose`; con `runtime.kind: podman-compose` hace falta host con capability `podman` (hoy pin de host).

## Por qué es el paso más rentable ahora

1. Envelope comercial v0.9 cerrado (usage + plan + flags).
2. Flujo secrets→apps + cutover Reelpath cerrados.
3. Adapter Podman existe; placement multi-capability sigue gap para Autopilot sin pin.

## Alcance concreto del incremento (siguiente)

1. Placement SHARED: filtrar hosts por capability requerida del manifiesto (`compose` / `podman`).
2. Sin Stripe / sin Redis-Kafka / sin SQL console proxy.

## Secundario (si sobra capacidad)

- Más meters (job minutes, backup GB).
- Hard-enforce soft limits (hoy solo reportan).

## Cola (no es el siguiente obligatorio)

1. Convención viva: secret `db.url` (+ `db.schema`); schema `app_<slug>`; envFrom → Compose ✅
2. Provisioner CREATE ROLE/SCHEMA + grants + UI metadata ✅
3. URLs/credenciales TTL (opción C) ✅
4. Feature flags / plan local ✅
5. Secrets usuario→app (UI rotate + docs ADR-0017) ✅; cutover Reelpath ✅
6. Adapter Podman opt-in ✅
7. Proxy+RLS (B) diferido

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): fases B–D + capabilities + sync + Podman adapter opt-in; Compose sigue default. K8s/systemd = adapters futuros.

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

> Autopilot SHARED elige host con la capability que pide el manifiesto (`podman` cuando `runtime.kind: podman-compose`); Compose default intacto; SSO/deploy/envFrom/DB/TTL/flags/secrets cutover sin romper.
