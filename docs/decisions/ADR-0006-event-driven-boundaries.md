# ADR-0006 — Event-driven solo en fronteras caras

- **Estado:** Accepted
- **Fecha:** 2026-07-27

## Contexto

¿Event sourcing / bus global para todo CRUD?

## Decisión

CRUD síncrono en Postgres. Eventos (outbox) para: ciclo de vida de deployment, host offline, secret rotated, audit projections, notificaciones, metering.

No event-sourcing. No choreography entre microservicios.

## Consecuencias

- (+) Simplicidad operativa.
- (+) Extensión (webhooks, billing) sin acoplar use cases.
- (−) No hay replay completo de estado desde eventos — no necesario.
