# ADR-003 — Prometheus como sistema de métricas

## Estado

Propuesta / **no verificada** (2026-07-26)

## Contexto

Briefing cita Prometheus + exporters. Config **NO ENCONTRADA** en Git.

## Decisión (declarada)

Prometheus como almacén y motor de scrape/alertas (con Alertmanager).

## Motivación típica

- Estándar de facto en métricas pull
- PromQL y ecosistema de exporters
- Integración nativa con Grafana

## Evidencia requerida

`prometheus.yml`, targets UP, retención, reglas, volumen TSDB.
