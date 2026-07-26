# ADR-0008 — Versionado API REST `/api/v1`

- **Estado:** Accepted
- **Fecha:** 2026-07-27

## Contexto

Clientes (SPA, automatizaciones) necesitan contrato estable mientras el dominio evoluciona (Application→Project).

## Decisión

- Prefijo `/api/v1` como versión actual.
- Cambios breaking → `/api/v2` (solo cuando sea inevitable).
- Evolución compatible en v1: campos nuevos opcionales, deprecations con periodo.
- Errores, paginación y auth documentados en `docs/api/`.

## Consecuencias

- (+) Compatibilidad SPA durante renames.
- (−) Endpoints deprecated temporales — coste aceptable.
