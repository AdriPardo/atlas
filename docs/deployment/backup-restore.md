# Backup & restore (Postgres lógico)

Atlas produce dumps lógicos con `pg_dump` (job `BACKUP_DATABASE`).

## Config

| Variable | Default | Notas |
|----------|---------|--------|
| `ATLAS_BACKUP_ENABLED` | `true` | Cron + admin enqueue |
| `ATLAS_BACKUP_DIR` | `/var/lib/atlas/backups` | Volumen `atlas_backups` |
| `ATLAS_BACKUP_KEEP_COUNT` | `7` | Conserva los N ficheros `atlas-*.sql.gz` más recientes |
| `ATLAS_BACKUP_CRON` | `0 30 2 * * *` | Diario 02:30 UTC (Spring cron 6 campos) |

Credenciales: mismas que `ATLAS_DB_*` / `spring.datasource.*`.

## Trigger manual (ADMIN)

```bash
curl -sS -X POST -H "Authorization: Bearer $TOKEN" \
  https://atlas.example/api/v1/admin/backup
# → 202 { id, type: BACKUP_DATABASE, status: PENDING, ... }
```

Seguir el job en `GET /api/v1/jobs/{id}` hasta `SUCCEEDED`. Artefacto: `$ATLAS_BACKUP_DIR/atlas-YYYYMMDD-HHMMSS.sql.gz`.

## Restore de prueba

1. Parar el backend (y cualquier cliente de la DB `atlas`).
2. Elegir un dump reciente en el volumen de backups.
3. Restaurar en una DB vacía (o tras drop/create del schema/DB de prueba):

```bash
# Ejemplo en Compose local: restaurar a un contenedor Postgres accesible
gunzip -c /var/lib/atlas/backups/atlas-YYYYMMDD-HHMMSS.sql.gz \
  | docker exec -i <postgres-container> \
    psql -U atlas -d atlas
```

4. Arrancar backend y verificar login + listado de projects.
5. En producción, preferir restore sobre una instancia/DB de staging antes de tocar la primaria.

## Notas

- La imagen backend incluye `postgresql-client` (`pg_dump`).
- El dump usa `--clean --if-exists --no-owner --no-acl`.
- No sustituye backups de volúmenes Docker de apps gestionadas; solo la DB de Atlas.
