# Requisitos de acceso para completar el inventario

Sin estos elementos, cualquier ficha de servicio, diagrama de flujo o runbook de recuperación sobre el host sería especulación.

## Mínimo necesario

1. **Host alcanzable** (IP o FQDN) desde el entorno del agente
2. **Credencial SSH** (clave privada o agente SSH) con permisos de lectura sobre:
   - árbol de compose / stacks (ruta real a confirmar)
   - `/etc/systemd/system` (units relevantes)
   - crontabs (`/etc/cron*`, `crontab -l`)
   - configs de Traefik, Prometheus, Grafana, Loki, Alloy, cloudflared
   - scripts de backup
3. Preferible: usuario con permiso para ejecutar:
   - `docker ps`, `docker inspect`, `docker network ls`, `docker volume ls`
   - `docker compose config` en cada stack
4. **Ruta canónica** de la infraestructura en el servidor (ej. `/opt/atlas`, `/srv/atlas`, home del usuario)

## Recomendación de configuración del entorno Cursor

Opciones (cualquiera basta):

| Opción | Descripción |
| --- | --- |
| A | Cloud Agent con **private worker** en el host Atlas |
| B | Secretos de entorno: `ATLAS_SSH_HOST`, `ATLAS_SSH_USER`, clave en `ATLAS_SSH_KEY` (o mount) |
| C | Importar al repo (sin secretos) los compose/configs y documentar el host aparte |

## Qué se ejecutará cuando haya acceso

Script: [`../../scripts/inventory/collect-host-inventory.sh`](../../scripts/inventory/collect-host-inventory.sh)

Salida esperada en `inventory/raw/host/<timestamp>/`.
