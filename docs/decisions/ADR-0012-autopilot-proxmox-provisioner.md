# ADR-0012 — Autopilot Proxmox VM provisioner (shared vs isolated)

- **Estado:** Accepted
- **Fecha:** 2026-07-27

## Contexto

Tras placement (ADR-0010) y Tunnel assist (ADR-0011), Autopilot aún decide “dónde” casi siempre como **shared LOCAL**. El siguiente hueco de producto es isolation: provisionar una VM Proxmox, registrarla como Host SSH/Docker, y reutilizar `DEPLOY_SERVICE`.

## Decisión

1. Puerto hexagonal `VmProvisionerPort` + adapter `ProxmoxVmProvisionerAdapter`.
2. `POST .../deploy` acepta `placementMode: SHARED | ISOLATED` (default `SHARED`).
3. `ISOLATED` llama al provisioner; si el resultado no es `CREATED`/`REUSED` con IP usable → **fallback a SHARED LOCAL** (deploy no falla).
4. Credenciales: `ATLAS_PROXMOX_*` + secret `proxmox.api.token` (`USER@REALM!TOKENID=UUID`).
5. Clone real solo si `ATLAS_PROXMOX_CLONE_ENABLED=true` **y** hay guest IP (`ATLAS_PROXMOX_DEFAULT_GUEST_IP` en este slice; agent en el siguiente).
6. Sin URL/token o con clone off: probe opcional de `/api2/json/version` y modo `STUBBED`.
7. Hosts UI permanece Advanced; no rewrite de Deploy/Jobs.

## Consecuencias

- (+) Decisión SHARED vs ISOLATED cableada end-to-end (API + UI + audit/payload).
- (+) Siguiente incremento puede habilitar clone + guest-agent sin re-arquitectar.
- (−) Hasta clone+IP, ISOLATED se comporta como SHARED (mensaje explícito en logs/audit).
- (−) Sync Host post-provision y wait-for-ready quedan para el siguiente thin slice.
