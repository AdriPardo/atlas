# ADR-0002 — Single-tenant por instalación

- **Estado:** Accepted (decisión producto 1A)
- **Fecha:** 2026-07-27

## Contexto

¿Multi-tenant SaaS o una org por deploy?

## Decisión

Cada instalación Atlas sirve a **una** organización. El negocio escala por **muchas instalaciones independientes**, no por tenants en una DB compartida SaaS.

Preparación futura: tabla `organizations` (1 fila), FKs `organization_id`, Teams dentro de la org. Multi-org en una install queda **post-v1.0** y no bloquea el diseño.

## Consecuencias

- (+) Seguridad y aislamiento simples; encaja self-hosting.
- (+) Menos complejidad de RLS/tenant filters en v0.x.
- (−) No hay “Atlas Cloud” multi-tenant sin trabajo posterior — consciente.
- (+) Miles de projects **dentro** de una install siguen siendo objetivo de schema/índices.
