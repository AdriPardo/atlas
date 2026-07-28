# Siguiente paso de implementación

## Estado del último incremento (completado)

**Proxmox VM reuse (`REUSED`)** (v0.8.5):

- ISOLATED: Host SSH existente por hostname canónico `atlas-…` → `REUSED` (sin Proxmox).
- Provisioner: match QEMU por nombre o tag = hostname; start si stopped; guest IP → `REUSED` (sin clone).
- Clone solo si no hay match y `ATLAS_PROXMOX_CLONE_ENABLED=true`; tags de clone incluyen hostname.
- ADR-0012 + UI hint + tests (`AutopilotPlacementService`, `ProxmoxVmProvisionerAdapter`).

**Previo:** runbook restore; DNS CNAME (ADR-0013); guest-ready 3b; Tunnel PUBLIC.

## Recomendación única (siguiente)

**Jobs stale `RUNNING` tras crash del worker** — recuperar o marcar FAILED/PENDING jobs huérfanos (p. ej. Reelpath) para que redeploy no quede bloqueado tras reinicio del worker.

## Por qué es el paso más rentable ahora

1. Reuse ISOLATED ya evita multiplicar VMs; el dolor operativo siguiente en dogfood es jobs stuck.
2. Slice pequeño y seguro en claim/recovery del worker (ADR-0005 / ADR-0009), sin tocar SSO.
3. ADR-0014 (manifiesto) sigue siendo norte; no bloquea recovery de jobs.

## Alcance concreto del incremento (stale RUNNING jobs)

1. Detectar jobs `RUNNING` con lease/heartbeat caducado (o worker id muerto).
2. Reclaim → `PENDING` o terminal `FAILED` con mensaje claro; documentar ops.
3. Test de recovery + nota en `docs/architecture/workers-queues.md`.

## Secundario (si sobra capacidad)

- Endurecer scopes de token Cloudflare documentados en UI Secrets hint.
- Primer slice de lectura de `atlas.yml` (ADR-0014 fase B) sin eliminar `composePath`.

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): el repo declara *cómo correr* en `atlas.yml`; Docker Compose es el adapter de hoy. Slice cuando toque desacoplar `composePath`.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka obligatorio.
- No motor completo de manifiesto ni eliminar `composePath` antes de migrar el deploy path (ADR-0014 fases B–D).
- No `compose down -v` ni tocar `.env` en runbooks de deploy.

## Definición de éxito (stale RUNNING jobs)

> Tras matar/reiniciar el worker, un job que quedó `RUNNING` se recupera (reclaim o FAILED) y un nuevo deploy puede encolarse y completar.
