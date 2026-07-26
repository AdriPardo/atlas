# Runbook — Recuperar Prometheus

**Estado:** BLOQUEADO

## Síntomas típicos

- Targets down masivos
- Contenedor unhealthy/restarting
- TSDB corrupta / disco lleno

## Pasos genéricos

1. `docker ps` / logs del contenedor Prometheus.
2. Comprobar espacio en disco del volumen TSDB.
3. Validar `prometheus.yml` (`promtool check config` si disponible).
4. Reiniciar contenedor solo si es seguro.
5. Si TSDB corrupta: restaurar backup del volumen (**backup NO ENCONTRADO** hoy).
6. Verificar targets UP y Alertmanager linkage.

## Evidencia requerida

Paths de volumen, compose, retención, backups.
