# Siguiente paso de implementación

## Estado del último incremento (completado)

**Autopilot slice 3b — Proxmox guest ready** (ADR-0012):

- Tras clone: wait UPID → start VM → poll qemu-guest-agent IPv4 (fallback `ATLAS_PROXMOX_DEFAULT_GUEST_IP`).
- Registrar Host SSH con secret `proxmox.ssh.private_key` + enqueue `SYNC_HOST`.
- `DEPLOY_SERVICE` usa ese Host cuando `placementMode=ISOLATED` y la VM queda ready.
- Props `ATLAS_PROXMOX_GUEST_READY_TIMEOUT_SECONDS` / `POLL_INTERVAL_MS`.

**Previo:** slice 3 SHARED/ISOLATED (`aa87f70`); Tunnel PUBLIC; placement; cron; Domains/Traefik.

## Recomendación única (siguiente)

**Autopilot DNS CNAME (Cloudflare)** — sobre Domains ACTIVE / PUBLIC: crear o actualizar CNAME hacia el Tunnel/hostname con token DNS de zona (`cloudflare.api.token` + zona), para cerrar el loop “Deploy PUBLIC → hostname resoluble” sin pegar records a mano.

## Por qué es el paso más rentable ahora

1. Guest-ready ya deja Isolated usable; el hueco siguiente en el journey Autopilot es DNS real (Tunnel Public Hostname ya está asistido).
2. Reutiliza Domains + secret Cloudflare existentes; thin slice sin rewrite.
3. Restore UI / runbook pueden ir en paralelo.

## Alcance concreto del incremento (DNS CNAME)

1. Puerto o extensión de `DnsProviderPort` / Cloudflare: upsert CNAME para Domain ACTIVE.
2. Cablear ensure en path PUBLIC deploy o acción explícita en Domain detail.
3. Documentar scopes del token DNS (zona) vs Tunnel API token.

## Secundario (si sobra capacidad)

- Runbook restore de prueba (`docs/deployment/backup-restore.md`).
- Reuse de VMs Proxmox (`REUSED`) por hostname/tag.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka obligatorio, no rewrite que elimine Hosts/Deployments.

## Definición de éxito (DNS CNAME)

> Deploy PUBLIC (o ensure Domain) deja un CNAME Cloudflare apuntando al Tunnel/target documentado; el hostname resuelve sin edición manual en Zero Trust DNS.
