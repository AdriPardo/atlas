# ADR-004 — Loki para logs

## Estado

Propuesta / **no verificada** (2026-07-26)

## Contexto

Briefing cita Loki (+ Alloy). Config **NO ENCONTRADA** en Git.

## Decisión (declarada)

Loki como backend de logs; Alloy como agente de recolección.

## Motivación típica

- Modelo label-based alineado con Prometheus
- Coste operativo menor que ELK para muchos self-hosted
- UI unificada en Grafana

## Evidencia requerida

Config Loki/Alloy, retención, almacenamiento, datasource Grafana.
