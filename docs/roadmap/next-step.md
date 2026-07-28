# Siguiente paso de implementación

## Estado del último incremento (completado)

**Runbook restore de prueba** (v0.8 continuidad):

- Runbook operador en `docs/deployment/backup-restore.md`: stop API → restore `atlas-*.sql.gz` → Flyway/health → smoke SSO/JWT.
- Checklist de verificación (health, auth, projects, secrets master key).
- Nota de continuidad en `docs/deployment/runtime.md`; criterio v0.8 marcado en `versions.md`.

**Previo:** DNS CNAME (ADR-0013); guest-ready 3b; Tunnel PUBLIC; ADR-0014 (norte, no código).

## Recomendación única (siguiente)

**Reuse de VMs Proxmox (`REUSED`)** — al elegir placement ISOLATED, reutilizar un Host/VM existente por hostname o tag en lugar de clonar siempre, para no multiplicar VMs en dogfood.

## Por qué es el paso más rentable ahora

1. Continuidad DB ya tiene runbook verificable; Autopilot ISOLATED aún clona por defecto.
2. Reuse baja coste operativo y alinea con “Hosts como Advanced” sin tirar el control plane.
3. ADR-0014 (manifiesto) sigue siendo norte; no bloquea reuse.

## Alcance concreto del incremento (Proxmox REUSED)

1. Resolver Host existente (hostname / tag Proxmox) → estado o semántica `REUSED` (sin clone).
2. Cablear en el path ISOLATED de `DEPLOY_SERVICE` (fallback a clone solo si no hay match).
3. Docs/ADR-0012 o nota corta + UI hint si aplica.

## Secundario (si sobra capacidad)

- Endurecer scopes de token Cloudflare documentados en UI Secrets hint.
- Primer slice de lectura de `atlas.yml` (ADR-0014 fase B) sin eliminar `composePath`.

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): el repo declara *cómo correr* en `atlas.yml`; Docker Compose es el adapter de hoy. Slice cuando toque desacoplar `composePath`.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka obligatorio.
- No motor completo de manifiesto ni eliminar `composePath` antes de migrar el deploy path (ADR-0014 fases B–D).
- No `compose down -v` ni tocar `.env` en runbooks de deploy.

## Definición de éxito (Proxmox REUSED)

> Un deploy ISOLATED encuentra una VM/Host reutilizable por hostname/tag, evita clone innecesario, y deja el servicio RUNNING en ese Host.
