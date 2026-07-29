# ADR-0004 — Evolución Application → Project / Service

- **Estado:** Accepted
- **Fecha:** 2026-07-27

## Contexto

El MVP modela `Application` como repo+compose+domain monolítico. Plataformas comerciales separan **Project** (producto/equipo) de **Service** (unidad desplegable).

## Decisión

Evolucionar sin big-bang:

1. Introducir `Project` + `Service` en dominio y schema.
2. Migrar cada `Application` → 1 Project + 1 Service.
3. Mantener `/api/v1/applications` deprecated un periodo.
4. UI: renombrar a Projects con redirect desde `/applications`.

No reescribir el frontend/backend de golpe.

## Consecuencias

- (+) Escala a monorepos / multi-service.
- (+) Roadmap claro.
- (−) Dualidad temporal de APIs — gestionar con Sunset headers y docs ([deprecations.md](../api/deprecations.md)). Sunset: **2027-08-01**.
