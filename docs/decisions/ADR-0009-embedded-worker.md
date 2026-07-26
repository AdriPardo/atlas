# ADR-0009 — Worker embebido en el proceso API (MVP)

- **Estado:** Accepted
- **Fecha:** 2026-07-27

## Contexto

v0.3/v0.4 necesita un worker de jobs sin añadir un segundo servicio Compose todavía.

## Decisión

1. El scheduler (`JobWorkerScheduler`) corre en el mismo Spring Boot process cuando `atlas.worker.enabled=true` (default).
2. La cola sigue siendo Postgres `SKIP LOCKED` ([ADR-0005](ADR-0005-workers-and-job-queue.md)).
3. Separación futura: misma imagen, flag `ATLAS_WORKER_ENABLED` distinto por replica.

## Consecuencias

- (+) Un solo contenedor para demo/prod pequeña.
- (+) Menos piezas que operar.
- (−) Un OOM/CPU spike de deploy afecta también a la API — mitigar separando replicas cuando haga falta.
