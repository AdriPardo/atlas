# Siguiente paso de implementación

## Estado del último incremento (completado)

**Autopilot slice 2 — PUBLIC Tunnel ingress assist** (ADR-0011):

- Puerto `CloudflareTunnelPort` + adapter (copy-ready ingress; API Zero Trust si hay credenciales).
- Endpoints `GET/POST /api/v1/domains/{id}/tunnel-ingress[/ensure]`.
- Tras deploy PUBLIC succeeded: ensure en logs (nunca tumba el job).
- UI Domains: **Tunnel** (copy) + **Ensure**.
- Props: `ATLAS_CF_ACCOUNT_ID` / `ATLAS_CF_TUNNEL_ID` / `ATLAS_CF_ZONE`; secret `cloudflare.api.token`.

**Previo:** Autopilot placement (host sin `hostId`, PUBLIC/INTERNAL); cron v0.8; Domains/Traefik; alerts; VIEWER/DEVELOPER.

## Recomendación única (siguiente)

**Autopilot slice 3 — Proxmox VM provisioner (thin)** — cuando placement decida “new VM”, crear/reusar VM vía API Proxmox, registrar Host, reutilizar `DEPLOY_SERVICE`. Hasta entonces placement sigue en shared LOCAL.

## Por qué es el paso más rentable ahora

1. Tunnel/PUBLIC ya reduce el gap de exposición; el hueco restante de Autopilot es **dónde** (shared vs isolated VM).
2. Infra prod ya tiene Proxmox; no requiere rewrite de Deploy/Jobs.
3. DNS CNAME Cloudflare real y restore UI pueden ir en paralelo después.

## Alcance concreto del incremento (Proxmox)

1. Puerto `VmProvisionerPort` + adapter Proxmox (URL + API token en settings/secrets).
2. Stub de política: shared LOCAL vs new VM (default LOCAL hasta que el provisioner esté listo).
3. Tras VM ready → Host SSH/Docker + Sync → enqueue `DEPLOY_SERVICE`.
4. No quitar Hosts UI (Advanced).

## Secundario (si sobra capacidad)

- DNS CNAME automático con token DNS (zona) sobre Domains ACTIVE.
- Runbook restore de prueba (`docs/deployment/backup-restore.md`).

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka obligatorio, no rewrite que elimine Hosts/Deployments.

## Definición de éxito (slice 3)

> Política “isolated” (o flag) provisiona VM Proxmox, registra Host, y el mismo job `DEPLOY_SERVICE` deja el servicio RUNNING.
