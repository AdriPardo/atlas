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

## Disco (HostDiskAlmostFull)

Alerta Prometheus: `<10%` libre en `/` (node-exporter). Causas habituales en platform:

| Origen | Acción segura |
|--------|----------------|
| Docker build cache / imágenes huérfanas | `docker builder prune`, `docker image prune`, `docker system prune` **sin** `--volumes` |
| journald | `journalctl --vacuum-time=7d` |
| `graphify-out/YYYY-MM-DD` viejos en checkout | Script conserva los 2 más recientes |
| `backend/target`, `frontend/build` | Borrados en cleanup (se regeneran en deploy) |

**Prohibido:** `docker volume prune` y cualquier borrado de volúmenes `reelpath-db`, `appClub`, postgres de plataforma, `/opt/atlas-data/*`.

### Limpieza periódica (systemd)

Tras `git pull` en `/opt/atlas/atlas`:

```bash
sudo bash /opt/atlas/atlas/scripts/install-disk-cleanup-timer.sh
```

| Parámetro | Valor |
|-----------|-------|
| Timer | Domingos 03:00 (±15 min aleatorio) |
| Umbral agresivo | ≥80% uso de `/` → prune imágenes/containers >7d + builder cache |
| Log | `/var/log/atlas-disk-cleanup.log` |

Prueba en seco:

```bash
DRY_RUN=1 /opt/atlas/atlas/scripts/disk-cleanup.sh
```

Ejecución manual:

```bash
sudo systemctl start atlas-disk-cleanup.service
```

Diagnóstico rápido:

```bash
df -h /
docker system df
du -sh /var/lib/docker/* 2>/dev/null | sort -hr | head
journalctl --disk-usage
```

Ajuste de alerta (opcional): subir umbral en Prometheus/Alertmanager si el cleanup semanal deja margen estable (>15% libre).
