# Módulos — Runtime

## Hosts

**MVP:** hostname, IP, OS, docker_version, online (manual).

**Objetivo:**
- Credenciales vía Secrets.
- `HostConnectorPort`: ping SSH, detectar Docker, listar compose projects.
- Job `SYNC_HOST` periódico.
- Labels/tags (env=prod, region=…, `runtime=compose|…` cuando ADR-0014 Fase D).
- Capacidad: max concurrent deploys.

**Nota:** Docker Compose es el runtime *actual* vía `ContainerRuntimePort`; la dirección de producto es orquestación por manifiesto + adapter ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)), no anclar el dominio a Docker.

## Containers

Vista de contenedores descubiertos en un host (nombre, image, status, ports). Read-mostly desde Docker API; acciones start/stop/restart con autorización y audit.

No es Portainer completo en v1: foco en contenedores **gestionados por Atlas** primero; “all containers” como vista avanzada ADMIN.

## Storage / Volumes

Inventario de volúmenes Docker relevantes a Services; binding a backups. Metadata en DB; datos en el host.

## Databases

Registro de instancias DB (Postgres/MySQL/…) usadas por projects:

- **Hoy:** connection string en Secrets del project (`db.url`); convención schema `app_<slug>` ([ADR-0015](../decisions/ADR-0015-project-database-access.md), [project-database-access.md](../product/project-database-access.md)).
- **Externas:** mismo binding; Atlas no provisiona el cluster remoto.
- **Gestionadas (post-billing):** CREATE ROLE/SCHEMA + grants `db.read` / `db.migrate` / `db.admin`; luego URLs TTL. SQL proxy diferido.

Útil para backups/restore de **apps** (futuro; distinto de `BACKUP_DATABASE` del control plane Atlas) y variables de conexión tipadas.

## Queues (infra del cliente)

Catálogo de brokers que corren en la flota (no la cola interna de Atlas). Deep-link health; no reimplementar RabbitMQ UI.

## Cron

Schedules que disparan jobs Atlas (`SYNC_HOST`, `BACKUP_VOLUME`, pipeline) o cron en contenedor documentado. UI: lista + next run + last status.
