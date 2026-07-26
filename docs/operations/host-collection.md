# Recolección de inventario en el host

## Objetivo

Obtener evidencia reproducible del servidor Atlas sin modificar su estado.

## Script

[`../../scripts/inventory/collect-host-inventory.sh`](../../scripts/inventory/collect-host-inventory.sh)

Modo local (ejecutado en el host):

```bash
./scripts/inventory/collect-host-inventory.sh --local --out inventory/raw/host
```

Modo remoto (desde el agente, cuando exista SSH):

```bash
export ATLAS_SSH_HOST="<host>"
export ATLAS_SSH_USER="<user>"
# opcional: ATLAS_SSH_KEY=/path/to/key
./scripts/inventory/collect-host-inventory.sh --remote --out inventory/raw/host
```

## Artefactos esperados

Bajo `inventory/raw/host/<timestamp>/`:

| Archivo | Contenido |
| --- | --- |
| `uname.txt` | Kernel / arch |
| `os-release.txt` | Distro |
| `disk.txt` | `df -h` |
| `docker-ps.txt` | Contenedores |
| `docker-inspect.json` | Inspect de contenedores |
| `docker-networks.txt` | Redes |
| `docker-volumes.txt` | Volúmenes |
| `compose-files.txt` | Rutas de compose halladas |
| `compose-tree/` | Copia de compose/config (sin secretos si se filtra) |
| `systemd-units.txt` | Units relacionadas |
| `cron.txt` | Crontabs |
| `listening-ports.txt` | Puertos en escucha |
| `NOTES.md` | Observaciones del operador |

## Reglas de seguridad

- No volcar secretos a Git (tokens Cloudflare, passwords Grafana, etc.)
- Redactar `.env` reales; preferir `.env.example` o listado de nombres de variables
- No reiniciar servicios durante la recolección
