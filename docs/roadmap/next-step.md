# Siguiente paso de implementación

## Estado del último incremento (completado)

**Autopilot DNS CNAME (Cloudflare)** (ADR-0013):

- `DnsProviderPort.ensureCname` + `CloudflareDnsAdapter` (upsert CNAME proxied → `{tunnel-id}.cfargotunnel.com`).
- Tras `DEPLOY_SERVICE` PUBLIC: Tunnel ensure + DNS CNAME ensure (nunca rompe el deploy).
- API `GET/POST …/dns-cname[/ensure]`; UI Domains **DNS** / **Ensure DNS**.
- Token `cloudflare.api.token` con Zone DNS Edit (+ Tunnel Edit si se comparte).

**Previo:** guest-ready 3b (`49c3a85`); Tunnel PUBLIC; SHARED/ISOLATED; Domains/Traefik.

## Recomendación única (siguiente)

**Runbook restore de prueba** — documentar y validar restore lógico Postgres (`docs/deployment/backup-restore.md`) sobre un backup real del stack, para cerrar el hueco de continuidad de v0.8 sin bloquear Autopilot.

## Por qué es el paso más rentable ahora

1. Autopilot PUBLIC ya cierra Tunnel + DNS; el gap operativo más barato es restore verificable.
2. Backup job ya existe; falta runbook + prueba documentada.
3. Reuse Proxmox VMs (`REUSED`) puede ir en paralelo si hay capacidad.

## Alcance concreto del incremento (restore runbook)

1. Runbook restore: stop API → restore dump → migrate/health → smoke SSO.
2. Checklist de verificación en `docs/deployment/backup-restore.md` (o crear si falta).
3. Nota en `runtime.md` / next-step success.

## Secundario (si sobra capacidad)

- Reuse de VMs Proxmox (`REUSED`) por hostname/tag.
- Endurecer scopes de token Cloudflare documentados en UI Secrets hint.

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): el repo declara *cómo correr* en `atlas.yml`; Docker Compose es el adapter de hoy, no el ancla de producto. Autopilot sigue dueño de placement, exposure, secrets, Traefik/Tunnel/DNS. Slice posterior (post-restore / cuando toque desacoplar `composePath`); no bloquear el runbook.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka obligatorio, no rewrite que elimine Hosts/Deployments.
- No motor completo de manifiesto ni eliminar `composePath` antes de migrar el deploy path (ADR-0014 fases B–D).

## Definición de éxito (restore runbook)

> Un operador puede restaurar un backup lógico de Atlas siguiendo el runbook y verificar health + login SSO/JWT sin improvisar comandos.
