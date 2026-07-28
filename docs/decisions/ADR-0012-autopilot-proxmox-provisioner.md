# ADR-0012 — Autopilot Proxmox VM provisioner (shared vs isolated)

- **Estado:** Accepted (slice 3b guest-ready + REUSED)
- **Fecha:** 2026-07-27 (REUSED 2026-07-28)

## Contexto

Tras placement (ADR-0010) y Tunnel assist (ADR-0011), Autopilot aún decide “dónde” casi siempre como **shared LOCAL**. El siguiente hueco de producto es isolation: provisionar una VM Proxmox, registrarla como Host SSH/Docker, y reutilizar `DEPLOY_SERVICE`.

## Decisión

1. Puerto hexagonal `VmProvisionerPort` + adapter `ProxmoxVmProvisionerAdapter`.
2. `POST .../deploy` acepta `placementMode: SHARED | ISOLATED` (default `SHARED`).
3. `ISOLATED` llama al provisioner; si el resultado no es `CREATED`/`REUSED` con IP usable **y** secret `proxmox.ssh.private_key` → **fallback a SHARED LOCAL** (deploy no falla).
4. Credenciales: `ATLAS_PROXMOX_*` + secret `proxmox.api.token` (`USER@REALM!TOKENID=UUID`).
5. **Reuse antes de clone** (`ATLAS_PROXMOX_REUSE_ENABLED=true` por defecto):
   - Placement: si ya existe un Host SSH con hostname canónico (`atlas-…`) e IP usable → `REUSED` (sin Proxmox).
   - Provisioner: lista QEMU en el nodo; match por **nombre** o **tag** Proxmox igual al hostname; arranca si está stopped; poll guest IP → `REUSED` (sin clone).
6. Clone real solo si no hay match y `ATLAS_PROXMOX_CLONE_ENABLED=true`: clone (tags = hostname [+ `ATLAS_PROXMOX_REUSE_TAG`]) → wait UPID → start → poll `agent/network-get-interfaces` (fallback `ATLAS_PROXMOX_DEFAULT_GUEST_IP`).
7. Tras VM ready: registrar Host SSH con `proxmox.ssh.private_key`, enqueue `SYNC_HOST`, y el deploy encola `DEPLOY_SERVICE` en ese Host.
8. Sin URL/token o sin match y clone off: probe opcional de `/api2/json/version` y modo `STUBBED`.
9. Hosts UI permanece Advanced; no rewrite de Deploy/Jobs.

## Consecuencias

- (+) SHARED vs ISOLATED + guest-ready end-to-end (API + UI + audit/payload).
- (+) Redeploy ISOLATED no multiplica VMs en dogfood (`REUSED` por hostname/tag).
- (+) Template cloud-init + agent + SSH key secret bastan para Isolated real.
- (−) Deploy ISOLATED puede bloquear hasta `ATLAS_PROXMOX_GUEST_READY_TIMEOUT_SECONDS` (default 120).
- (−) Sync y Deploy se encolan en paralelo; el worker de deploy reintenta si Docker aún no responde.
