# Siguiente paso de implementación

## Recomendación única

**Implementar el camino de deploy real mínimo (núcleo de v0.3→v0.4): cola de jobs en PostgreSQL + worker + adapters Git/SSH-Docker que conviertan un `Deployment` de registro manual en una ejecución real de `docker compose` en un Host.**

## Por qué es el paso más rentable

1. **Desbloquea la promesa del producto** — hoy Atlas “administra” pero no despliega; sin esto sigue siendo un inventario con SSO.
2. **Reutiliza el dominio MVP** — `Deployment`, `Host`, `Application`/`Service` y los puertos `Unsupported*` ya existen; no requiere big-bang Project todavía (puede ir en paralelo o justo antes como v0.2 corto).
3. **Justifica workers sin over-engineering** — Postgres `SKIP LOCKED` ([ADR-0005](../decisions/ADR-0005-workers-and-job-queue.md)); Redis aún no.
4. **Máximo aprendizaje de infra real** — credenciales, redes Docker, Traefik labels, fallos parciales: alimenta el resto del diseño (Secrets, Logs, Alerts).
5. **Demo comercial** — un compose que pasa a RUNNING desde la UI es la prueba de que Atlas no es un toy.

## Alcance concreto del incremento

1. Tabla `jobs` + use cases enqueue/claim.
2. Worker process o perfil Spring que procese `DEPLOY_SERVICE` y `SYNC_HOST`.
3. Adapter SSH/Docker: sync host metadata; `compose pull/up` en path remoto o via Docker context.
4. Adapter Git: clone/fetch branch al workspace del host o del worker.
5. Actualizar `Deployment.status` + `logs` durante la ejecución.
6. UI: botón **Deploy** en detalle + poll de estado/logs (sin rediseño grande).
7. Tests de integración del job claim; test del adapter con Testcontainers Docker si es viable, o contract test del puerto.

## Qué no hacer en este incremento

- No marketplace, no multi-tenant, no Kafka, no rewrite a Project (salvo que se elija un spike v0.2 de un día para renombrar API).
- No Billing, no AI, no Portainer-complete.
- No exponer backend sin Traefik.

## Orden sugerido si se parte el trabajo

```text
A. jobs table + worker skeleton (v0.3 slice)
B. Secrets mínimos para SSH key / Git token
C. SYNC_HOST real
D. DEPLOY_SERVICE real end-to-end
E. (siguiente) migración Project/Service formal
```

## Definición de éxito

> Desde la UI, un operador elige Application/Service + Host, pulsa Deploy, y en menos de unos minutos el stack compose está arriba en el host con logs visibles y status `SUCCEEDED` o `FAILED` honestos — sin SSH manual.

Cuando este diseño se valide, **ese** es el primer ticket de implementación.
