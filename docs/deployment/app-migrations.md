# App migrations — Atlas no posee el ORM

## Regla

Atlas **no** impone Prisma, Flyway, Liquibase ni ningún migrator. La plataforma:

1. Inyecta env de DB (`DATABASE_URL` u otras keys que el repo declare en Compose / `.env`).
2. Si el manifiesto declara `runtime.migrateCommand`, el deploy **ejecuta ese comando** tal cual (shell en el workspace del host), **después** de `compose up` / `RuntimeOrchestratorPort.apply`.
3. Si el campo falta, Atlas **no** corre migraciones de app (el contenedor puede migrar solo en su entrypoint).

El migrator, el orden de scripts y la idempotencia son **responsabilidad del repo**.

## Manifiesto

```yaml
runtime:
  kind: compose
  composeFile: docker-compose.atlas.yml
  # Opcional — hook estructurado (recomendado para Prisma/Flyway en contenedor api):
  migration:
    enabled: true
    strategy: prisma          # prisma | flyway | custom
    container: api              # default api — docker compose exec -T
    command: npm run migrate:deploy -w @autotube/database
  # Legacy (shell en workspace, sin wrap exec):
  # migrateCommand: npm run db:migrate:deploy
```

Ejemplos válidos (Atlas no interpreta el contenido del migrator):

| App | Config tipica |
|-----|----------------|
| Prisma (Reelpath/Autotube) | `strategy: prisma`, `container: api`, `command: npm run migrate:deploy -w @autotube/database` |
| Flyway (CLI en imagen) | `strategy: flyway`, `container: api` |
| Django / shell libre | `strategy: custom`, `command: docker compose exec -T api python manage.py migrate --noinput` (o `migrateCommand` legacy) |

### Override en Atlas UI (Service)

En **Project → Deploy → Migrations** puedes fijar por servicio (sin tocar el repo):

| Campo | Uso |
|-------|-----|
| `migrationEnabled` | `true` fuerza migrate post-deploy; `false` lo desactiva aunque el manifiesto lo declare |
| `migrationStrategy` | `prisma` \| `flyway` \| `custom` |
| `migrationCommand` | Comando inner (p. ej. Reelpath: `npm run migrate:deploy -w @autotube/database`) |
| `migrationContainer` | Servicio Compose (default `api`) — Atlas envuelve con `docker compose exec -T` |

Prioridad: **Service override** → `runtime.migration` → `runtime.migrateCommand` (legacy).

## Orden en el deploy

1. `git clone/update`
2. Seed `.env` (si no existe)
3. Resolver `runtime.composeFile` + `envFrom.secretRef`
4. Inyectar secrets declarados en `.env` (`db.url` → `DATABASE_URL`; no loguea valores)
5. `compose up` (DB y servicios)
6. **Si** hay migración resuelta (`runtime.migration`, legacy `migrateCommand`, o Service override) → ejecutar shell en workspace (LOCAL o SSH). Con `container` declarado, Atlas usa `docker compose exec -T <container> sh -c '<command>'`.
7. Fallo de migrate → deploy **FAILED** (logs en deployment).
8. Tunnel / DNS Autopilot

DB del stack ya está arriba antes del hook (healthchecks Compose suelen bastar). El comando debe poder alcanzar la DB (hostname de red Docker, `docker compose exec`, etc.).

### `envFrom.secretRef`

```yaml
runtime:
  composeFile: docker-compose.atlas.yml
  envFrom:
    - secretRef: db.url          # → DATABASE_URL
    - secretRef: db.schema       # → DB_SCHEMA
    - secretRef: ai.openai       # → OPENAI_API_KEY (user secret in Atlas; ADR-0017)
    - secretRef: ai.elevenlabs   # → ELEVENLABS_API_KEY
    - secretRef: custom.token
      env: MY_TOKEN              # override key
```

También se leen `services.*.envFrom` (unión; `runtime` gana si choca la misma env key). Secret ausente → warn + skip (deploy no falla). Flujo secrets→apps: [secrets-for-apps.md](../product/secrets-for-apps.md).

## Evitar doble migrate

Dos patrones válidos; **elige uno**:

| Patrón | Manifiesto | Entrypoint contenedor |
|--------|------------|------------------------|
| **A — hook Atlas** | `migrateCommand` declarado | No correr migrate otra vez |
| **B — migrate on start** | **omitir** `migrateCommand` | `migrate && start` (idempotente) |

**Reelpath / Autotube (patrón A — hook Atlas):**

1. Quitar migrate del entrypoint del contenedor `api` (evitar doble apply).
2. En `atlas.yml`:

```yaml
runtime:
  migration:
    enabled: true
    strategy: prisma
    container: api
    command: npm run migrate:deploy -w @autotube/database
```

O en Atlas UI (Service → Migrations): `migrationEnabled=true`, strategy `prisma`, container `api`, mismo command.

**Fallback manual** (SSH al host, cwd workspace del deployment):

```bash
cd /var/lib/atlas/workspaces/<deployment-id>
docker compose -f docker-compose.atlas.yml exec -T api sh -c 'npm run migrate:deploy -w @autotube/database'
```

Prisma `migrate deploy` es idempotente; doble corrida en cada redeploy sigue siendo ruido ops. Preferir un solo dueño (entrypoint **o** hook Atlas).
