# ADR-0012 — Autopilot Proxmox VM provisioner (shared vs isolated)

- **Estado:** Accepted (slice 3b guest-ready)
- **Fecha:** 2026-07-27

## Contexto

Tras placement (ADR-0010) y Tunnel assist (ADR-0011), Autopilot aún decide “dónde” casi siempre como **shared LOCAL**. El siguiente hueco de producto es isolation: provisionar una VM Proxmox, registrarla como Host SSH/Docker, y reutilizar `DEPLOY_SERVICE`.

## Decisión

1. Puerto hexagonal `VmProvisionerPort` + adapter `ProxmoxVmProvisionerAdapter`.
2. `POST .../deploy` acepta `placementMode: SHARED | ISOLATED` (default `SHARED`).
3. `ISOLATED` llama al provisioner; si el resultado no es `CREATED`/`REUSED` con IP usable **y** secret `proxmox.ssh.private_key` → **fallback a SHARED LOCAL** (deploy no falla).
4. Credenciales: `ATLAS_PROXMOX_*` + secret `proxmox.api.token` (`USER@REALM!TOKENID=UUID`).
5. Clone real solo si `ATLAS_PROXMOX_CLONE_ENABLED=true`: clone → wait UPID → start → poll `agent/network-get-interfaces` (fallback `ATLAS_PROXMOX_DEFAULT_GUEST_IP`).
6. Tras VM ready: registrar Host SSH con `proxmox.ssh.private_key`, enqueue `SYNC_HOST`, y el deploy encola `DEPLOY_SERVICE` en ese Host.
7. Sin URL/token o con clone off: probe opcional de `/api2/json/version` y modo `STUBBED`.
8. Hosts UI permanece Advanced; no rewrite de Deploy/Jobs.

## Consecuencias

- (+) SHARED vs ISOLATED + guest-ready end-to-end (API + UI + audit/payload).
- (+) Template cloud-init + agent + SSH key secret bastan para Isolated real.
- (−) Deploy ISOLATED puede bloquear hasta `ATLAS_PROXMOX_GUEST_READY_TIMEOUT_SECONDS` (default 120).
- (−) Sync y Deploy se encolan en paralelo; el worker de deploy reintenta si Docker aún no responde.
