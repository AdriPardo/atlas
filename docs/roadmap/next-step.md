# Siguiente paso de implementación

## Estado del último incremento (completado)

**Billing meters `job.minutes` + `backup.gb`:**

- `UsageMeters.JOB_MINUTES` / `BACKUP_GB`; entitlements soft unlimited (community/enterprise).
- Complete/fail/stale job → wall-clock minutes vía `BillingMeterPort` (dims: jobId, jobType, status).
- Backup dump → GiB (6 decimales) + dims path/bytes; meter soft (no falla job).
- UI Billing empty-state menciona los tres meters; export CSV sin cambio de schema.

**Previo:** Autopilot SHARED placement por capability; Reelpath secrets; Adapter Podman; ADR-0017; Secrets/envFrom; DB provisioner/TTL; feature flags; PUBLIC minify + TLS; Host sync; OpenAPI; Pipeline `hostId`; `migrateCommand`; Autopilot Tunnel/DNS/Proxmox.

## Recomendación única (siguiente)

**Hard-enforce soft limits de billing** — hoy meters reportan; plan community aún no bloquea al cruzar límite finito (projects/hosts).

## Por qué es el paso más rentable ahora

1. Envelope meters (`deploy.count`, `job.minutes`, `backup.gb`) + gauges cerrados.
2. Soft limits sin dientes = commercial envelope incompleto.
3. Stripe / Redis-Kafka / SQL console siguen fuera.

## Alcance concreto del incremento (siguiente)

1. Gate en flujos sensibles (create project/host, opcional enqueue deploy) cuando usage/gauge ≥ limit y `soft=true` aún: o bien rechazo claro, o flag `ATLAS_BILLING_ENFORCE`.
2. Sin Stripe / sin Redis-Kafka / sin SQL console proxy.

## Secundario (si sobra capacidad)

- Totales period agregados en `GET /billing/usage` (sum by meter).

## Cola (no es el siguiente obligatorio)

1. Convención viva: secret `db.url` (+ `db.schema`); schema `app_<slug>`; envFrom → Compose ✅
2. Provisioner CREATE ROLE/SCHEMA + grants + UI metadata ✅
3. URLs/credenciales TTL (opción C) ✅
4. Feature flags / plan local ✅
5. Secrets usuario→app (UI rotate + docs ADR-0017) ✅; cutover Reelpath ✅
6. Adapter Podman opt-in ✅
7. Placement SHARED por capability (`compose`/`podman`) ✅
8. Meters job minutes + backup GB ✅
9. Proxy+RLS (B) diferido

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

> Create project/host (y opcional deploy) rechaza o avisa de forma controlada al cruzar soft limit; SSO/deploy/placement/secrets/meters sin romper; enforce opt-in o documentado.
