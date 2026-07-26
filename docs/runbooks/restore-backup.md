# Runbook — Restaurar un backup

**Estado:** BLOQUEADO — no hay scripts ni destinos de backup en Git

## Evidencia actual

**NO ENCONTRADO** en repositorio. Jobs cron de backup: **BLOQUEADO** sin host.

## Procedimiento genérico (no validado en Atlas)

1. Identificar servicio y tipo de dato (volumen Docker vs bind mount vs DB dump).
2. Detener el servicio afectado.
3. Restaurar archivos al path/volumen correcto.
4. Verificar ownership/permisos.
5. Arrancar servicio y validar health.
6. Validar integridad funcional (login Grafana, queries Prometheus, etc.).

## Acción requerida

Tras recolección, reemplazar este runbook con comandos y paths reales; enlazar desde cada ficha de servicio.
