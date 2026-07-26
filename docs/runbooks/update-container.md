# Runbook — Actualizar un contenedor

**Estado:** plantilla

## Pasos

1. Leer ficha del servicio en `docs/services/`.
2. Registrar imagen/digest actual (`docker inspect`).
3. Cambiar tag/digest en compose.
4. `docker compose pull <service>`
5. `docker compose up -d <service>`
6. Verificar healthcheck y logs.
7. Observar métricas/alertas 15–30 min.
8. Actualizar documentación de versión.

## Rollback

Reponer tag anterior en compose y `up -d` de nuevo.
