# Siguiente paso de implementación

## Estado del último incremento (completado)

**Auto-deploy on git push** (v0.8.7):

- One-click `POST /api/v1/pipelines/enable-auto-deploy` + panel en Project detail.
- Registro opcional de webhook GitHub vía API cuando existe `git.token`.
- Filtro webhook: solo `push` a la branch del service (ignora ping/PR/otras ramas).
- Docs + tests.

**Previo:** Stale RUNNING job recovery (v0.8.6); Proxmox VM reuse; DNS CNAME; guest-ready; Tunnel PUBLIC.

## Recomendación única (siguiente)

**Endurecer scopes de token Cloudflare documentados en UI Secrets hint** — o primer slice de lectura de `atlas.yml` (ADR-0014 fase B) sin eliminar `composePath`.

## Por qué es el paso más rentable ahora

1. Auto-deploy cierra la fricción “push → redeploy” (Reelpath / dogfood).
2. Cloudflare scopes reduce fricción de dogfood PUBLIC (Tunnel + DNS).
3. ADR-0014 sigue siendo norte; no bloquea ops diarios.

## Alcance concreto del incremento (siguiente)

1. Documentar en UI Secrets los scopes mínimos Cloudflare (Zone DNS Edit + Tunnel/Cloudflare One Edit).
2. Opcional: slice lectura `atlas.yml` (ADR-0014 fase B) si sobra capacidad.

## Secundario (si sobra capacidad)

- Primer slice de lectura de `atlas.yml` (ADR-0014 fase B) sin eliminar `composePath`.
- Host opcional en Pipeline (Autopilot en cada run webhook) en lugar de `hostId` pinneado.

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): el repo declara *cómo correr* en `atlas.yml`; Docker Compose es el adapter de hoy. Slice cuando toque desacoplar `composePath`.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka obligatorio.
- No motor completo de manifiesto ni eliminar `composePath` antes de migrar el deploy path (ADR-0014 fases B–D).
- No `compose down -v` ni tocar `.env` en runbooks de deploy.

## Definición de éxito (siguiente)

> Operador ve en Secrets UI qué scopes necesita el token Cloudflare; Tunnel/DNS assist no falla por scopes mal documentados.
