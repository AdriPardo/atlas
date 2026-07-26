# Arquitectura — Eventos

## Filosofía

Eventos **donde el desacoplo paga**: notificar UI, audit, webhooks, alertas derivadas. **No** event-sourcing del CRUD. El estado canónico vive en PostgreSQL.

## Bus interno (v0.x)

1. Use case escribe entidad + fila en `outbox_events` (misma TX).
2. Publicador polling / listen NOTIFY publica a:
   - handlers in-process, o
   - Redis pub/sub / stream (fase B), o
   - webhooks HTTP salientes.
3. Marca outbox como publicado.

Sin outbox al principio está permitido **solo** para eventos best-effort (métricas). Audit y “deployment finished → notify” deben ser fiables → outbox.

## Catálogo de eventos de dominio (diseño)

| Evento | Agregado | Consumidores típicos |
|--------|----------|----------------------|
| `ProjectCreated` | Project | Audit, dashboard cache invalidate |
| `ServiceUpdated` | Service | Audit |
| `DeploymentQueued` | Deployment | Worker enqueue |
| `DeploymentStarted` | Deployment | UI live, metrics |
| `DeploymentSucceeded` | Deployment | Notify, audit, domain verify |
| `DeploymentFailed` | Deployment | Alert, notify, audit |
| `HostWentOffline` | Host | Alert |
| `SecretRotated` | Secret | Audit (sin valor) |
| `UserProvisionedFromSso` | User | Audit |
| `AlertFired` | Alert | Notifications |
| `BackupCompleted` | Backup | Audit, notify |

Payload: id + tipo + timestamp + actor + datos mínimos (no blobs de logs).

## Integración con Observability

- Contadores Micrometer por evento publicado/consumido.
- Correlación: `traceId` / `deploymentId` en MDC.

## Webhooks salientes (v0.8+)

`WebhookEndpoint` por instalación (URL, secret HMAC, filtros de evento). Reintentos con backoff en jobs.

## Qué no hacer

- Choreography compleja entre 10 microservicios.
- Duplicar estado solo en el bus.
- Publicar secretos o tokens en payloads.
