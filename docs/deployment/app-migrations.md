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
  # Opcional. Solo si quieres un hook explícito de plataforma:
  migrateCommand: npm run db:migrate:deploy
```

Ejemplos válidos (Atlas no interpreta el contenido):

| App | `migrateCommand` tipico |
|-----|-------------------------|
| Prisma | `npm run db:migrate:deploy` |
| Flyway (CLI en imagen) | `docker compose -f docker-compose.atlas.yml run --rm api flyway migrate` |
| Django | `docker compose exec -T api python manage.py migrate --noinput` |

## Orden en el deploy

1. `git clone/update`
2. Seed `.env` (si no existe)
3. Resolver `runtime.composeFile` + `envFrom.secretRef`
4. Inyectar secrets declarados en `.env` (`db.url` → `DATABASE_URL`; no loguea valores)
5. `compose up` (DB y servicios)
6. **Si** hay `runtime.migrateCommand` → ejecutarlo en el workspace (LOCAL o SSH)
7. Tunnel / DNS Autopilot

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

**Reelpath hoy:** patrón B — `api` corre `npm run migrate:deploy:ci` al arrancar. Mientras eso siga, **no** poner `migrateCommand` en `atlas.yml` (evita doble apply innecesario). Cuando el repo quite migrate del entrypoint, declarar el hook.

Prisma `migrate deploy` suele ser idempotente, pero doble corrida en cada redeploy sigue siendo ruido y riesgo ops. Preferir un solo dueño.
