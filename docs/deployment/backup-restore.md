# Backup & restore (Postgres lógico)

Atlas produce dumps lógicos con `pg_dump` (job `BACKUP_DATABASE`). Este documento es el runbook operativo para **tomar** un backup y **restaurarlo** de forma verificable (criterio v0.8).

> **Alcance:** solo la base de datos de Atlas (metadatos de projects, hosts, secrets cifrados, jobs, domains…).  
> No cubre volúmenes Docker de apps gestionadas (`atlas_workspaces` sí se listan abajo como fuera de alcance de este dump).

## Config

| Variable | Default | Notas |
|----------|---------|--------|
| `ATLAS_BACKUP_ENABLED` | `true` | Cron + admin enqueue |
| `ATLAS_BACKUP_DIR` | `/var/lib/atlas/backups` | Volumen `atlas_backups` |
| `ATLAS_BACKUP_KEEP_COUNT` | `7` | Conserva los N ficheros `atlas-*.sql.gz` más recientes |
| `ATLAS_BACKUP_CRON` | `0 30 2 * * *` | Diario 02:30 UTC (Spring cron 6 campos) |
| `ATLAS_BACKUP_PG_DUMP_BINARY` | `pg_dump` | Debe existir en la imagen backend (`postgresql-client`) |

Credenciales: mismas que `ATLAS_DB_*` / `spring.datasource.*`.

Formato del artefacto: `$ATLAS_BACKUP_DIR/atlas-YYYYMMDD-HHMMSS.sql.gz`  
Flags del dump: `--clean --if-exists --no-owner --no-acl` (idempotente al reaplicar SQL).

## Trigger manual (ADMIN)

```bash
curl -sS -X POST -H "Authorization: Bearer $TOKEN" \
  https://atlas.example/api/v1/admin/backup
# → 202 { id, type: BACKUP_DATABASE, status: PENDING, ... }
```

Seguir el job en `GET /api/v1/jobs/{id}` hasta `SUCCEEDED`.

Listar dumps desde el contenedor backend (volumen montado):

```bash
# Dev (docker-compose.yml) o prod con servicio backend
docker compose exec backend ls -lt /var/lib/atlas/backups/atlas-*.sql.gz | head
```

En producción con compose file propio:

```bash
cd /opt/atlas/atlas
docker compose -f docker-compose.prod.yml exec backend \
  ls -lt /var/lib/atlas/backups/atlas-*.sql.gz | head
```

---

## Runbook: restore de prueba (operador)

Objetivo: restaurar un dump lógico y demostrar que el control plane vuelve sano (health + auth) **sin improvisar comandos**.

Preferir siempre restore sobre **staging** o una DB de prueba antes de tocar la primaria. En prod, planificar ventana corta (API parada).

### 0. Precondiciones

- [ ] Dump elegido: `atlas-YYYYMMDD-HHMMSS.sql.gz` (tamaño > 0; fecha conocida).
- [ ] Credenciales DB (`ATLAS_DB_USERNAME` / `ATLAS_DB_PASSWORD` / DB name) disponibles.
- [ ] `ATLAS_SECRETS_MASTER_KEY` y `ATLAS_JWT_SECRET` del entorno restaurado son los **mismos** que cuando se tomó el dump (si no, secrets at-rest y JWT fallan).
- [ ] Acceso Docker al host Atlas (o `psql` al Postgres compartido).
- [ ] Ventana acordada / aviso a operadores (API down).

Identificar nombres (ajustar si tu compose project name difiere):

| Pieza | Dev (`docker-compose.yml`) | Prod típico (`docker-compose.prod.yml`) |
|-------|----------------------------|----------------------------------------|
| Compose | `docker compose` | `docker compose -f docker-compose.prod.yml` |
| API | servicio `backend` | servicio `backend` |
| Postgres | servicio `postgres` (mismo compose) | host `postgres` en red `atlas-internal` (stack infra) |

### 1. Parar el API (y clientes de la DB)

Evita escrituras concurrentes y locks durante el restore.

```bash
# Desde el directorio del repo en el host
COMPOSE="docker compose"   # prod: docker compose -f docker-compose.prod.yml

$COMPOSE stop backend
# Opcional: frontend puede seguir sirviendo estáticos, pero no tendrá API
```

Comprobar que no quedan conexiones de Atlas a la DB (opcional):

```bash
# Dev — postgres en el mismo compose
$COMPOSE exec postgres \
  psql -U atlas -d atlas -c "SELECT pid, usename, state, query FROM pg_stat_activity WHERE datname = 'atlas';"

# Prod — Postgres compartido (contenedor/host según tu stack infra)
docker exec -i <postgres-container> \
  psql -U atlas -d atlas -c "SELECT pid, usename, state FROM pg_stat_activity WHERE datname = 'atlas';"
```

### 2. Elegir y copiar el dump si hace falta

Si el dump solo está en el volumen del backend:

```bash
DUMP=atlas-YYYYMMDD-HHMMSS.sql.gz
$COMPOSE exec backend ls -la /var/lib/atlas/backups/$DUMP

# Copiar al host (opcional, para inspeccionar o restaurar vía otro contenedor)
docker cp "$($COMPOSE ps -q backend):/var/lib/atlas/backups/$DUMP" /tmp/$DUMP
```

### 3. Restaurar el dump

El SQL incluye `DROP … IF EXISTS` / recreación de objetos. Restaurar **contra la DB `atlas`** con el usuario de aplicación.

**Dev (Postgres del compose):**

```bash
DUMP=atlas-YYYYMMDD-HHMMSS.sql.gz

$COMPOSE exec -T backend \
  sh -c "gunzip -c /var/lib/atlas/backups/$DUMP" \
  | $COMPOSE exec -T postgres \
    psql -U atlas -d atlas -v ON_ERROR_STOP=1
```

**Prod (Postgres compartido en `atlas-internal`):**

```bash
DUMP=atlas-YYYYMMDD-HHMMSS.sql.gz
PG_CONTAINER=<postgres-container>   # p.ej. el del stack /opt/atlas/infrastructure

$COMPOSE exec -T backend \
  sh -c "gunzip -c /var/lib/atlas/backups/$DUMP" \
  | docker exec -i "$PG_CONTAINER" \
    psql -U atlas -d atlas -v ON_ERROR_STOP=1
```

Si `psql` no está en el backend y el dump ya está en el host:

```bash
gunzip -c /tmp/$DUMP \
  | docker exec -i "$PG_CONTAINER" \
    psql -U atlas -d atlas -v ON_ERROR_STOP=1
```

Errores esperables a vigilar: permisos (`must be owner`), DB inexistente, o dump truncado. Con `-v ON_ERROR_STOP=1` el pipe falla en el primer error SQL grave.

### 4. Arrancar backend (Flyway + health)

```bash
$COMPOSE start backend
# o: $COMPOSE up -d backend
```

Flyway corre al boot. Si el dump ya incluye el schema al nivel del código desplegado, las migraciones aplicadas se saltan; si el código es más nuevo, aplicará solo las pendientes.

Esperar health:

```bash
# Loopback en la VM / host
until curl -sf http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"'; do
  sleep 2
done
echo "backend UP"
```

Compose healthcheck equivalente: `wget -qO- http://localhost:8080/actuator/health | grep -q UP`.

### 5. Smoke auth (SSO / JWT) + datos

Elegir el camino según el perfil del entorno.

**A — Login local** (`ATLAS_AUTHENTIK_ENABLED=false` o fallback password):

```bash
TOKEN=$(curl -sS -X POST http://127.0.0.1:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$ATLAS_ADMIN_USERNAME\",\"password\":\"$ATLAS_ADMIN_PASSWORD\"}" \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

test -n "$TOKEN" && echo "login OK"
curl -sS -H "Authorization: Bearer $TOKEN" \
  http://127.0.0.1:8080/api/v1/projects | head -c 200
echo
```

**B — SSO Authentik** (perfil `docker` en prod; headers ForwardAuth o sesión Traefik):

```bash
# Tras login por el IdP / Traefik, desde un cliente que reciba los headers:
curl -sS -o /dev/null -w "%{http_code}\n" \
  -H "X-authentik-username: <usuario-conocido>" \
  -H "X-authentik-groups: Atlas Admins" \
  http://127.0.0.1:8080/api/v1/auth/sso
# Esperado: 200 + JWT en body
```

Smoke UI: abrir `https://atlas.example` → sesión Authentik → Dashboard → listado de projects conocido del dump.

### 6. Checklist de verificación (cerrar el restore)

Marcar todo antes de dar el restore por bueno:

| # | Check | Cómo |
|---|--------|------|
| 1 | Dump aplicado sin error de pipe | exit code 0 de `psql -v ON_ERROR_STOP=1` |
| 2 | Backend `UP` | `GET /actuator/health` → `"status":"UP"` |
| 3 | Auth funciona | login local **o** `/auth/sso` 200 + JWT |
| 4 | Datos coherentes | `GET /api/v1/projects` lista proyectos esperados del dump |
| 5 | Secrets legibles | abrir un secret conocido en UI (misma `ATLAS_SECRETS_MASTER_KEY`) |
| 6 | Worker / jobs | opcional: `POST /api/v1/admin/backup` encola y llega a `SUCCEEDED` |
| 7 | SSO UI (prod) | login por Authentik → Dashboard sin 401 en bucle |

Si falla (4) o (5) con health OK: suele ser dump equivocado o master key distinta — **no** “arreglar” rotando keys a ciegas; restaurar el dump correcto o recuperar la key del entorno original.

---

## Práctica recomendada (staging)

1. Tomar backup en prod (`POST /admin/backup` o cron).
2. Copiar el `.sql.gz` a staging.
3. Ejecutar este runbook contra la DB de staging.
4. Completar el checklist.
5. Documentar fecha, dump usado y resultado (ticket / nota de ops).

Eso cierra el criterio v0.8: *backup programado + restore de prueba documentado*.

---

## Notas

- La imagen backend incluye `postgresql-client` (`pg_dump`); `gunzip`/`psql` para restore pueden vivir en backend + contenedor Postgres.
- El dump **no** incluye `atlas_workspaces` ni imágenes Docker de customer apps.
- API CRUD `/backups` + `POST /backups/{id}/restore` (endpoints.md v0.8) es superficie futura; el camino soportado hoy es este runbook + job `BACKUP_DATABASE`.
- Tras restore en primaria, evitar arrancar una segunda réplica API contra la misma DB con worker habilitado hasta confirmar un solo writer (`ATLAS_WORKER_ENABLED`).
