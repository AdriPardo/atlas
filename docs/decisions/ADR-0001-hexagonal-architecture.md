# ADR-0001 — Arquitectura hexagonal

- **Estado:** Accepted
- **Fecha:** 2026-07-27

## Contexto

Atlas crecerá con muchos adapters (Docker, SSH, Git, Prometheus, Cloudflare). La lógica de negocio no debe acoplarse a Spring Web ni JPA.

## Decisión

Mantener y reforzar el multi-módulo:

`domain` → `application` (ports/use cases) → `infrastructure` + `bootstrap`.

Nuevas capacidades = nuevos paquetes de dominio + ports + adapters; no microservicios prematuros.

## Consecuencias

- (+) Testabilidad y límites claros.
- (+) Sustitución de adapters (`Unsupported*` → real).
- (−) Más archivos/boilerplate que un CRUD Spring monolítico clásico — aceptable.
