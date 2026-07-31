# Siguiente paso de implementación

## Estado del último incremento (completado)

**Adapter Podman (ADR-0014) — opt-in vía `RuntimeOrchestratorPort`:**

- `PodmanRuntimeOrchestratorAdapter` → `podman compose up/down`; `RoutingRuntimeOrchestratorAdapter` enruta por capability.
- Opt-in: `atlas.yml` `runtime.kind: podman-compose` + host con capability `podman` (sync ya la anuncia).
- Compose default intacto (`kind` omitido / `compose`).
- Tests: routing, podman adapter, deploy job, resolver capability.

**Previo:** ADR-0017 secrets usuario→apps (`6a040d2`); Secrets project + bindings; envFrom inject; DB provisioner/TTL; feature flags; PUBLIC minify + TLS; Host sync; OpenAPI; Pipeline `hostId`; `migrateCommand`; Autopilot Tunnel/DNS/Proxmox.

## Recomendación única (siguiente)

**Reelpath cutover secrets:** prefer env Atlas (`envFrom` / `.env` inject) sobre `PlatformSecret` in-app — cuando el repo Reelpath esté a mano. Alternativa: Autopilot SHARED placement por capability pedida (hoy SHARED filtra solo `compose`).

## Por qué es el paso más rentable ahora

1. Envelope comercial v0.9 cerrado (usage + plan + flags).
2. Flujo secrets→apps cerrado en Atlas; cutover app = trabajo aparte.
3. Adapter Podman cierra gap deploy; placement multi-capability y K8s siguen opcionales.

## Alcance concreto del incremento (siguiente)

1. Reelpath: prefer env Atlas → fallback PlatformSecret; `atlas.yml` envFrom con keys del usuario.
2. Sin Stripe / sin Redis-Kafka / sin SQL console proxy.

## Secundario (si sobra capacidad)

- Autopilot SHARED: filtrar por capability requerida (podman cuando manifiesto lo pida) — hoy pin de host.
- Más meters (job minutes, backup GB).
- Hard-enforce soft limits (hoy solo reportan).

## Cola (no es el siguiente obligatorio)

1. Convención viva: secret `db.url` (+ `db.schema`); schema `app_<slug>`; envFrom → Compose ✅
2. Provisioner CREATE ROLE/SCHEMA + grants + UI metadata ✅
3. URLs/credenciales TTL (opción C) ✅
4. Feature flags / plan local ✅
5. Secrets usuario→app (UI rotate + docs ADR-0017) ✅; cutover Reelpath pendiente
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

> Reelpath (u otra app) lee secrets desde env inject Atlas; PlatformSecret solo fallback; SSO/deploy/envFrom/DB/TTL/flags/Podman opt-in sin romper.
