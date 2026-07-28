# Siguiente paso de implementación

## Estado del último incremento (completado)

**Stale RUNNING job recovery** (v0.8.6):

- Heartbeat de lease (`locked_at`) mientras el worker ejecuta un job.
- Reclaim al arranque + tick periódico: `RUNNING` con lease > `ATLAS_JOB_STALE_TIMEOUT` → `FAILED` (`FOR UPDATE SKIP LOCKED`).
- Cascade en `DEPLOY_SERVICE`: deployment PENDING/RUNNING → FAILED; service/project DEPLOYING → FAILED.
- Docs: `docs/architecture/workers-queues.md`; tests unitarios + integración.

**Previo:** Proxmox VM reuse (`REUSED`); runbook restore; DNS CNAME (ADR-0013); guest-ready 3b; Tunnel PUBLIC.

## Recomendación única (siguiente)

**Endurecer scopes de token Cloudflare documentados en UI Secrets hint** — o primer slice de lectura de `atlas.yml` (ADR-0014 fase B) sin eliminar `composePath`.

## Por qué es el paso más rentable ahora

1. Recovery de jobs stale cierra el bloqueo operativo post-crash (Reelpath / redeploy).
2. Cloudflare scopes reduce fricción de dogfood PUBLIC (Tunnel + DNS).
3. ADR-0014 sigue siendo norte; no bloquea ops diarios.

## Alcance concreto del incremento (siguiente)

1. Documentar en UI Secrets los scopes mínimos Cloudflare (Zone DNS Edit + Tunnel/Cloudflare One Edit).
2. Opcional: slice lectura `atlas.yml` (ADR-0014 fase B) si sobra capacidad.

## Secundario (si sobra capacidad)

- Primer slice de lectura de `atlas.yml` (ADR-0014 fase B) sin eliminar `composePath`.

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): el repo declara *cómo correr* en `atlas.yml`; Docker Compose es el adapter de hoy. Slice cuando toque desacoplar `composePath`.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka obligatorio.
- No motor completo de manifiesto ni eliminar `composePath` antes de migrar el deploy path (ADR-0014 fases B–D).
- No `compose down -v` ni tocar `.env` en runbooks de deploy.

## Definición de éxito (siguiente)

> Operador ve en Secrets UI qué scopes necesita el token Cloudflare; Tunnel/DNS assist no falla por scopes mal documentados.
