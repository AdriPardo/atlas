# Siguiente paso de implementación

## Estado del último incremento (completado)

**Cloudflare token scopes in Secrets UI** (v0.8.8):

- Hint en Org secrets + Project secrets: Zone DNS Edit + Tunnel / Cloudflare One Edit para `cloudflare.api.token`.
- HelperText dinámico al crear/vincular ese nombre.
- Docs (`config-security`, public hostname) alineados.

**Previo:** Auto-deploy on git push (v0.8.7); stale RUNNING recovery; Proxmox REUSED; DNS CNAME; Tunnel PUBLIC.

## Recomendación única (siguiente)

**Primer slice de lectura de `atlas.yml` (ADR-0014 fase B)** — parsear manifiesto del repo en deploy path **sin eliminar** `composePath` (fallback si no hay `atlas.yml`).

## Por qué es el paso más rentable ahora

1. Scopes CF en UI cierran fricción dogfood PUBLIC (Tunnel + DNS).
2. ADR-0014 es el norte; fase B desbloquea “repo declara cómo correr” sin romper Compose.
3. No bloquea ops diarios: `composePath` sigue siendo el default.

## Alcance concreto del incremento (siguiente)

1. Definir schema mínimo `atlas.yml` (runtime `compose` + path/service hints) según ADR-0014.
2. En deploy: si el checkout tiene `atlas.yml` válido → usarlo; si no → `composePath` actual.
3. Tests + docs; **no** eliminar columnas/API `composePath`.

## Secundario (si sobra capacidad)

- Host opcional en Pipeline (Autopilot en cada run webhook) en lugar de `hostId` pinneado.
- UX Domains: mensaje explícito si Ensure falla por 403 (scopes insuficientes).

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): el repo declara *cómo correr* en `atlas.yml`; Docker Compose es el adapter de hoy. Fases C–D (desacoplar / eliminar `composePath`) después de migrar el deploy path.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka obligatorio.
- No motor completo de manifiesto ni eliminar `composePath` antes de migrar el deploy path (ADR-0014 fases B–D).
- No `compose down -v` ni tocar `.env` en runbooks de deploy.

## Definición de éxito (siguiente)

> Deploy de un service con `atlas.yml` en el repo usa ese manifiesto; sin archivo, el path Compose existente sigue funcionando igual.
