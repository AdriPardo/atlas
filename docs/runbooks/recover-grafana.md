# Runbook — Recuperar Grafana

**Estado:** BLOQUEADO

## Pasos genéricos

1. Logs del contenedor Grafana.
2. Comprobar volumen `/var/lib/grafana` (o path real TBD).
3. Verificar DB SQLite/Postgres según config.
4. Restaurar backup del volumen si corrupción.
5. Validar login, datasources Prometheus/Loki, dashboards.

## Evidencia requerida

Compose, auth, provisioning, backups.
