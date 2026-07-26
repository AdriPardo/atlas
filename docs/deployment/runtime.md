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
       ├─ backend (internal)
       ├─ worker (internal)     ← añadir cuando haya jobs reales
       ├─ postgres (internal)   ← o DB compartida con DB name `atlas`
       └─ redis (internal)      ← desde fase B
```

## Redes

| Red | Quién |
|-----|-------|
| `atlas-public` | frontend (Traefik → UI) |
| `atlas-internal` | backend, worker, postgres, redis |

Backend **no** publicado a Internet; solo vía proxy UI o router Traefik interno con ForwardAuth si se expone API directa.

## Configuración

Variables: ver `.env.example` (`ATLAS_DB_*`, `ATLAS_JWT_*`, `ATLAS_AUTHENTIK_*`, admin seed, CORS).

Perfil `docker`: SSO on. Secretos de producción distintos de defaults.

## Persistencia

- Volumen `atlas_pg_data`.
- Futuro: volumen para artifacts/backup staging.
- Credenciales host/SSH: en DB cifradas, no en imágenes.

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
