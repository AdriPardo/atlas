# ADR-0005 — Workers y cola (Postgres primero)

- **Estado:** Accepted
- **Fecha:** 2026-07-27

## Contexto

Deploys reales no pueden vivir en el request HTTP. ¿Redis/Kafka desde día 1?

## Decisión

1. **Fase A:** tabla `jobs` + `FOR UPDATE SKIP LOCKED` en PostgreSQL; proceso/perfil `worker`.
2. **Fase B:** Redis para cache y/o Streams cuando la carga o live-logs lo justifiquen.
3. **No** Kafka en v0–v1.

Rationale: cero infra nueva al desbloquear valor; misma TX que outbox; suficiente para escala self-hosted típica.

## Consecuencias

- (+) Entrega más rápida de deploys reales.
- (+) Operación simple.
- (−) Migrar a Redis Streams después implica dual-write temporal — planificado.
