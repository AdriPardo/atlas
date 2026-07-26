# ADR-0007 — Observabilidad por integración

- **Estado:** Accepted
- **Fecha:** 2026-07-27

## Contexto

Ya existen Prometheus, Grafana, Loki. Duplicarlos dentro de Atlas sería coste absurdo.

## Decisión

Atlas emite métricas/logs propios y **consulta/enlaza** el stack externo. Almacena metadata de alertas producto y preferencias UI, no TSDB.

## Consecuencias

- (+) Reutiliza inversión ops del cliente.
- (+) UI puede deep-link a Grafana.
- (−) Dependencia de configuración externa (URLs, credenciales) — Settings module.
