# Siguiente paso de implementación

## Estado del último incremento (completado)

**Autopilot SHARED placement por capability pedida:**

- Peek `atlas.yml` al enqueue (workspace `placement/{serviceId}`) → `requiredRuntimeCapability`.
- SHARED filtra hosts por `compose` / `podman`; sin host `podman` → error claro (no seed compose-only).
- Pin `hostId` sigue override; Compose default intacto.
- Soft-fallback a `compose` si peek git falla (job sigue validando capability post-clone).

**Previo:** Reelpath cutover secrets; Adapter Podman; ADR-0017; Secrets/envFrom; DB provisioner/TTL; feature flags; PUBLIC minify + TLS; Host sync; OpenAPI; Pipeline `hostId`; `migrateCommand`; Autopilot Tunnel/DNS/Proxmox.

## Recomendación única (siguiente)

**Más meters de billing (job minutes, backup GB)** — envelope v0.9 tiene `deploy.count`; soft limits aún solo reportan.

## Por qué es el paso más rentable ahora

1. Placement multi-capability + Podman adapter + secrets cutover cerrados.
2. Envelope comercial incompleto en meters / hard-enforce.
3. Stripe / Redis-Kafka / SQL console siguen fuera.

## Alcance concreto del incremento (siguiente)

1. Meters adicionales: job minutes y/o backup GB vía `BillingMeterPort`.
2. Sin Stripe / sin Redis-Kafka / sin SQL console proxy.

## Secundario (si sobra capacidad)

- Hard-enforce soft limits (hoy solo reportan).

## Cola (no es el siguiente obligatorio)

1. Convención viva: secret `db.url` (+ `db.schema`); schema `app_<slug>`; envFrom → Compose ✅
2. Provisioner CREATE ROLE/SCHEMA + grants + UI metadata ✅
3. URLs/credenciales TTL (opción C) ✅
4. Feature flags / plan local ✅
5. Secrets usuario→app (UI rotate + docs ADR-0017) ✅; cutover Reelpath ✅
6. Adapter Podman opt-in ✅
7. Placement SHARED por capability (`compose`/`podman`) ✅
8. Proxy+RLS (B) diferido

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): fases B–D + capabilities + sync + Podman adapter + placement multi-capability; Compose sigue default. K8s/systemd = adapters futuros.

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

> Usage export incluye meters job minutes y/o backup GB; plan/flags/SSO/deploy/placement/secrets sin romper.
