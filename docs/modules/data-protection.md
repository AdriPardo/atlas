# Módulos — Data protection

## Backups

Jobs que archivan volúmenes o dumps DB registrados. Destinos: filesystem en host de backup, S3-compatible (futuro).

Entidad `Backup`: source, status, size, checksum, location ref, created_at, expires_at.

## Restore

Flujo guiado: elegir Backup → target host/volume → job `RESTORE_*` → verificación. Requiere confirmación y rol elevado; audit obligatorio.

## Políticas

Retention (N días / N copias), schedule Cron, cifrado en tránsito/reposo según destino.
