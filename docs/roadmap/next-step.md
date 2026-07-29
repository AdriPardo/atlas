# Siguiente paso de implementación

## Estado del último incremento (completado)

**Lectura de `atlas.yml` en deploy (ADR-0014 fase B / v0.8.9):**

- Tras clone: si existe `atlas.yml` (o alias `atlas.project.yml`) válido → `runtime.composeFile` alimenta `composeUp`.
- Sin manifiesto (o sin `composeFile`) → fallback a `Service.composePath` (sin eliminar columna/API).
- `runtime.kind` omitido / `compose` / `podman-compose` OK; otros kinds → error claro.
- Tests: loader, resolver, deploy job.

**Previo:** Cloudflare token scopes in Secrets UI (v0.8.8); Auto-deploy on git push; stale RUNNING recovery; Proxmox REUSED; DNS CNAME; Tunnel PUBLIC.

## Recomendación única (siguiente)

**Fase C ligera (ADR-0014):** UI/API dejan de *exigir* `composePath` cuando el repo trae `atlas.yml` con `runtime.composeFile`; campo DB pasa a opcional / derivado. Repos solo-compose sin manifiesto: Atlas sintetiza manifiesto mínimo en memoria.

## Por qué es el paso más rentable ahora

1. Fase B ya lee el manifiesto en el hot path; falta alinear contrato de producto (create project no fuerza path Compose si hay manifest).
2. Desbloquea “repo declara cómo correr” de punta a punta sin rewrite de orchestrator.
3. Aún no toca Hosts / Traefik / Tunnel.

## Alcance concreto del incremento (siguiente)

1. `composePath` opcional en create/update Service cuando hay manifiesto (o default sintetizado).
2. UI New Project: path Compose opcional si documentamos `atlas.yml`.
3. Tests + docs; **aún no** renombrar `ContainerRuntimePort` ni eliminar columna.

## Secundario (si sobra capacidad)

- Host opcional en Pipeline (Autopilot en cada run webhook) en lugar de `hostId` pinneado.
- UX Domains: mensaje explícito si Ensure falla por 403 (scopes insuficientes).

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): fases C–D (desacoplar / eliminar `composePath`, port genérico). Compose sigue adapter default.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka obligatorio.
- No motor completo de manifiesto ni eliminar `composePath` de DB antes de migrar callers (fase C–D).
- No `compose down -v` ni tocar `.env` en runbooks de deploy.

## Definición de éxito (siguiente)

> Crear/actualizar service sin `composePath` obligatorio cuando el repo declara `runtime.composeFile`; deploys legacy con solo `composePath` siguen verdes.
