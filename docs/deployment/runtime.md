# Deployment — Runtime en infra existente

## Qué ya funciona (no romper)

- Proxmox VE como hipervisor / VMs.
- Docker + redes `atlas-public` / `atlas-internal`.
- Traefik + Cloudflare Tunnel + HTTPS.
- Authentik SSO (ForwardAuth).
- PostgreSQL compartido o dedicado (Compose actual trae Postgres propio).
- Prometheus, Grafana, Loki, Node Exporter.

Atlas Compose (`docker-compose.yml`): `postgres`, `backend`, `frontend`.

## Topología recomendada producción

```text
VM / host Docker
  ├─ traefik (existente)
  ├─ authentik (existente)
  ├─ observability stack (existente)
  └─ atlas stack
       ├─ frontend (public network)
       ├─ backend+worker (internal)  ← MVP: worker embebido en el mismo proceso
       ├─ worker (internal)          ← opcional: proceso/replica separado más adelante
       ├─ postgres (internal)        ← o DB compartida con DB name `atlas`
       └─ redis (internal)           ← desde fase B
```

### Worker embebido (MVP) vs proceso separado

Por defecto `ATLAS_WORKER_ENABLED=true` en el mismo contenedor/proceso API (`JobWorkerScheduler` + `@Scheduled`).

Para separar más adelante:

1. Replica API: `ATLAS_WORKER_ENABLED=false`
2. Replica worker: mismo jar, `ATLAS_WORKER_ENABLED=true` (puede omitir exposición HTTP o usar perfil `worker`)
3. Ambos comparten Postgres; el claim `SKIP LOCKED` evita doble ejecución

No hace falta un segundo Dockerfile todavía.
## Redes

| Red | Quién |
|-----|-------|
| `atlas-public` | frontend (Traefik → UI) |
| `atlas-internal` | backend, worker, postgres, redis |

Backend **no** publicado a Internet; solo vía proxy UI o router Traefik interno con ForwardAuth si se expone API directa.

## Configuración

Variables: ver `.env.example` (`ATLAS_DB_*`, `ATLAS_JWT_*`, `ATLAS_AUTHENTIK_*`, `ATLAS_SECRETS_MASTER_KEY`, `ATLAS_WORKER_*`, `ATLAS_WORKSPACE_DIR`, admin seed, CORS, opcionales `ATLAS_CF_*` / `ATLAS_PROXMOX_*`).

Perfil `docker`: SSO on. Secretos de producción distintos de defaults.

Autopilot ISOLATED (Proxmox): `ATLAS_PROXMOX_API_URL` / `NODE` / `TEMPLATE_VMID` + secrets `proxmox.api.token` y `proxmox.ssh.private_key`. Clone off por defecto (`ATLAS_PROXMOX_CLONE_ENABLED=false`); con clone on, Atlas espera guest-agent (o `ATLAS_PROXMOX_DEFAULT_GUEST_IP`). Ver [ADR-0012](../decisions/ADR-0012-autopilot-proxmox-provisioner.md).

## Persistencia

- Volumen `atlas_pg_data`.
- Volumen `atlas_workspaces` (clones Git + compose working dirs).
- Volumen `atlas_backups` para dumps lógicos (`pg_dump`); ver [backup-restore.md](./backup-restore.md).
- Credenciales host/SSH: en DB cifradas, no en imágenes (`ATLAS_SECRETS_MASTER_KEY` en prod).

### Continuidad (backup / restore)

Runbook operador: parar `backend` → restaurar `atlas-*.sql.gz` con `psql -v ON_ERROR_STOP=1` → arrancar (Flyway) → `/actuator/health` UP → smoke login SSO/JWT + `GET /projects`. Checklist completo en [backup-restore.md](./backup-restore.md). Misma `ATLAS_SECRETS_MASTER_KEY` que al tomar el dump.

## Hosts LOCAL y Docker socket

Para sync/deploy con `connectionType=LOCAL` en la misma VM:

1. `docker-compose.prod.yml` (gitignored; partir de `docker-compose.prod.yml.example`) monta `/var/run/docker.sock` y `group_add: ${DOCKER_GID}` (GID de `getent group docker`).
2. El contenedor `backend` incluye Docker CLI + compose plugin.
3. **Seguridad:** el socket otorga control total del engine del host. Solo en instalaciones single-tenant de confianza.
4. Repos privados: secret lógico `git.token` (PAT GitHub con scope `repo`). Resolución: binding alias del proyecto → secret owned del proyecto → secret organization (`POST /api/v1/secrets` o UI Org secrets). Preferible crear/vincular en el Project detail.
5. Cuidado con choques de puertos host (`:3000` ya lo usa Atlas frontend).

Dev (`docker-compose.yml`): el socket va comentado; descomentar + `group_add` solo si necesitas LOCAL en local.

## Recursos orientativos (single VM)

| Servicio | CPU | RAM |
|----------|-----|-----|
| backend | 1 | 512MB–1GB |
| worker | 1–2 | 512MB–2GB (builds) |
| frontend | 0.2 | 64MB |
| postgres | 1 | 1GB+ |

Ajustar según flota gestionada (Atlas gestiona otros hosts; él mismo puede ser ligero).

## Actualización

1. Pull imagen / rebuild.
2. Flyway migrate on boot.
3. Rolling: worker drain → API → UI.
4. Healthchecks Compose/Traefik antes de cortar tráfico.

## Separación DB compartida

Si se usa Postgres compartido del homelab/org:

- Database `atlas`, user least-privilege.
- No mezclar schemas con Authentik/Grafana.
- Backups independientes del DB Atlas.
