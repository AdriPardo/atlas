# Módulos — Runtime

## Hosts

**MVP:** hostname, IP, OS, docker_version, online (manual).

**Objetivo:**
- Credenciales vía Secrets.
- `HostConnectorPort`: ping SSH, detectar Docker, listar compose projects.
- Job `SYNC_HOST` periódico.
- Labels/tags (env=prod, region=…).
- Capacidad: max concurrent deploys.

## Containers

Vista de contenedores descubiertos en un host (nombre, image, status, ports). Read-mostly desde Docker API; acciones start/stop/restart con autorización y audit.

No es Portainer completo en v1: foco en contenedores **gestionados por Atlas** primero; “all containers” como vista avanzada ADMIN.

## Storage / Volumes

Inventario de volúmenes Docker relevantes a Services; binding a backups. Metadata en DB; datos en el host.

## Databases

Registro de instancias DB (Postgres/MySQL/…) usadas por projects:

- Externas (connection string en Secrets).
- Gestionadas (compose profile Atlas-known) — fase posterior.

Útil para backups/restore y variables de conexión tipadas.

## Queues (infra del cliente)

Catálogo de brokers que corren en la flota (no la cola interna de Atlas). Deep-link health; no reimplementar RabbitMQ UI.

## Cron

Schedules que disparan jobs Atlas (`SYNC_HOST`, `BACKUP_VOLUME`, pipeline) o cron en contenedor documentado. UI: lista + next run + last status.
