# Platform VM — RAM y herramientas admin

Host: `192.168.1.24` (Proxmox VMID 100 `platform`). IaC live: `/opt/atlas/infrastructure/docker/` (no está en este repo app).

## Memoria Proxmox

| Parámetro | Valor |
|-----------|-------|
| `memory` (max) | 8192 MiB |
| `balloon` (mín. protegido) | 6144 MiB (subido desde 4096) |
| Guest visible (tras deflate QMP) | ~6–7 GiB |

Si el guest vuelve a ~3.7 GiB bajo presión del nodo: `qm set 100 --balloon 6144` y, si hace falta, QMP `balloon` a 6 GiB. No bajar balloon sin mirar `free` en Proxmox (`192.168.1.20`).

## Caps Docker (observabilidad)

| Servicio | Límite | Notas |
|----------|--------|-------|
| cadvisor | 256 MiB | `--docker_only`, housekeeping 30s, métricas reducidas |
| prometheus | 400 MiB | retention 15d / 4 GB |
| alloy | 256 MiB | |
| loki | 256 MiB | datos en `/opt/atlas-data/loki` intactos |
| dozzle | 128 MiB | |
| uptime-kuma | 256 MiB | data `/opt/atlas-data/uptime-kuma` |

No hacer `docker volume prune`. Build cache prune OK (`docker builder prune`).

## URLs (SSO Authentik, grupo Atlas Admins)

| App | URL |
|-----|-----|
| Dozzle | https://dozzle.atlasops.dev |
| Uptime Kuma | https://uptime.atlasops.dev |

Compose: `.../docker/dozzle`, `.../docker/uptime-kuma`. Arranque: `docker compose up -d` en cada dir.

Uptime Kuma: primer login pide setup admin local (además del ForwardAuth).
