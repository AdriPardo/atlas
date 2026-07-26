# Runbook — Recuperar Loki

**Estado:** BLOQUEADO

## Pasos genéricos

1. Logs Loki + Alloy.
2. Disco/retention del storage Loki.
3. Validar config (YAML).
4. Reinicio controlado.
5. Restore de storage si aplica.
6. Query de prueba desde Grafana.

## Evidencia requerida

Config, volumen, retención, backups.
