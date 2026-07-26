# Runbook — Investigar una alerta

**Estado:** plantilla — reglas/receivers **NO ENCONTRADOS**

## Pasos

1. Capturar: nombre alerta, severity, labels, `startsAt`.
2. En Alertmanager: silences / inhibitions activos.
3. En Prometheus: expresión de la regla, valor actual, graph.
4. En Grafana: dashboard asociado (si existe).
5. En Loki: logs del servicio en la ventana de la alerta.
6. En host: `docker ps`, `docker logs`, recursos (`df`, `free`).
7. Clasificar: incidente real / flapping / falta de capacidad / falso positivo.
8. Mitigar y documentar en postmortem breve.

## Gap

Sin reglas en Git no hay mapa alerta → runbook. Completar tras inventario.
