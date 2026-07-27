# Siguiente paso de implementación

## Estado del último incremento (completado)

**Autopilot slice 3 — Proxmox VM provisioner (thin)** (ADR-0012):

- Puerto `VmProvisionerPort` + `ProxmoxVmProvisionerAdapter` (probe `/version`; clone opt-in).
- `placementMode: SHARED | ISOLATED` en deploy (default SHARED); UI toggle Shared / Isolated.
- ISOLATED sin credenciales / clone off / sin guest IP → fallback SHARED LOCAL (no tumba deploy).
- Props `ATLAS_PROXMOX_*`; secret `proxmox.api.token`.

**Previo:** Tunnel PUBLIC (ADR-0011); placement host+exposure (ADR-0010); cron v0.8; Domains/Traefik; alerts.

## Recomendación única (siguiente)

**Autopilot slice 3b — Proxmox guest ready** — tras clone: esperar qemu-guest-agent / cloud-init IP, registrar Host SSH, Sync, enqueue `DEPLOY_SERVICE` en esa VM. Activar `ATLAS_PROXMOX_CLONE_ENABLED=true` en prod con template cloud-init.

## Por qué es el paso más rentable ahora

1. La decisión SHARED/ISOLATED ya está cableada; falta el “VM ready” para que ISOLATED no caiga a LOCAL.
2. Infra prod ya tiene Proxmox; no requiere rewrite de Deploy/Jobs.
3. DNS CNAME Cloudflare real y restore UI pueden ir en paralelo.

## Alcance concreto del incremento (guest ready)

1. Poll IP vía guest-agent (o DHCP lease) tras clone.
2. Registrar Host SSH + secret key + Sync.
3. Enqueue `DEPLOY_SERVICE` al Host nuevo.
4. No quitar Hosts UI (Advanced).

## Secundario (si sobra capacidad)

- DNS CNAME automático con token DNS (zona) sobre Domains ACTIVE.
- Runbook restore de prueba (`docs/deployment/backup-restore.md`).

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka obligatorio, no rewrite que elimine Hosts/Deployments.

## Definición de éxito (slice 3b)

> Deploy con `placementMode=ISOLATED` provisiona VM Proxmox, registra Host con IP real, y el mismo job `DEPLOY_SERVICE` deja el servicio RUNNING en esa VM.
