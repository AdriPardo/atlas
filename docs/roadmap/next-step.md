# Siguiente paso de implementación

## Estado del último incremento

**Autopilot Placement (slice 1)** — producto + thin path:

- Docs: `docs/product/autopilot-placement.md`, ADR-0010.
- Deploy sin `hostId` obligatorio: auto-selección / seed de Host LOCAL `atlas-local`.
- `exposure` PUBLIC|INTERNAL en Service; PUBLIC crea stub Domain + Traefik metadata; INTERNAL no.
- UI project detail: CTA Deploy + toggle de exposición; Hosts como Advanced.

**Previo en árbol:** v0.7 Alerts + Domains/Traefik metadata; VIEWER/DEVELOPER ACL; v0.8b backups; Git webhooks.

## Recomendación única (siguiente)

**Autopilot slice 2 — Proxmox VM provisioner** — cuando el placement decida “new VM”, crear VM en Proxmox, registrar Host (SSH/Docker), y reutilizar el mismo `DEPLOY_SERVICE`.

## Por qué es el paso más rentable ahora

1. Cierra el hueco de la decision tree (reuse vs provision) sin reescribir Deploy/Jobs.
2. Encaja con infra prod ya presente (Proxmox) sin tocar Tunnel/SSO.
3. Cron schedules, DNS Cloudflare real y polish VIEWER quedan como secundarios tras placement usable.

## Alcance concreto del incremento (Proxmox)

1. Puerto `VmProvisionerPort` + adapter Proxmox (API token / template).
2. Política: cuándo provisionar vs reutilizar shared host (capacidad / flag de aislamiento).
3. Tras VM ready → `Host` SSH + Sync → enqueue `DEPLOY_SERVICE`.
4. No Cloudflare DNS API real aún; no quitar Hosts UI.

## Secundario (si sobra capacidad)

- v0.8 Cron schedules sobre el worker embebido.
- Cloudflare DNS sync real sobre Domains ACTIVE.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka obligatorio, no rewrite que elimine Hosts/Deployments.

## Definición de éxito (slice 2)

> Deploy con política “isolated” provisiona VM Proxmox, registra Host, y el mismo job `DEPLOY_SERVICE` deja el servicio RUNNING.
