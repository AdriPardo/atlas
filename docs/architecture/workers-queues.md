# Arquitectura — Workers y colas

## Cuándo justificar workers

| Trabajo | ¿Worker? | Motivo |
|---------|----------|--------|
| CRUD / lecturas API | No | Síncrono HTTP |
| Deploy (git pull + compose) | **Sí** | Minutos, I/O, fallos parciales |
| Build de imagen | **Sí** | CPU/tiempo |
| Health sync de hosts | **Sí** (cron/job) | Flota grande |
| Emitir alertas | Opcional | Si evaluación es pesada; si no, regla en Prometheus |
| Generar backups | **Sí** | Largo |
| Render dashboard stats | No (query) | Cache Redis opcional |

Sin jobs de larga duración, **no** introducir Redis ni brokers.

## Fases recomendadas

### Fase A — Postgres job queue (v0.3–v0.4)

Tabla `jobs`:

- `id`, `type`, `payload` (JSONB), `status`, `attempts`, `available_at`, `locked_by`, `locked_at`, `last_error`, timestamps.

Claim:

```sql
UPDATE jobs
SET status = 'RUNNING', locked_by = :worker, locked_at = NOW()
WHERE id = (
  SELECT id FROM jobs
  WHERE status = 'PENDING' AND available_at <= NOW()
  ORDER BY created_at
  FOR UPDATE SKIP LOCKED
  LIMIT 1
)
RETURNING *;
```

**Rationale:** cero infra nueva; suficiente para decenas de deploys concurrentes; transacciones y outbox en la misma DB.

Proceso: mismo artefacto JAR con perfil `worker` (o thread pool en API al inicio — migrar a proceso separado antes de producción seria).

### Fase B — Redis (v0.6+)

Introducir Redis cuando:

- Cache de listados/dashboard calientes, o
- Necesidad de pub/sub para UI en tiempo casi real (deploy logs), o
- Cola con mayor throughput / múltiples prioridades.

Opciones:

| Tecnología | Uso | Nota |
|------------|-----|------|
| Redis + lista/stream | Cola de jobs | Streams + consumer groups preferible a LPOP simple |
| Redis como cache | Stats, session secundaria | TTL explícitos |
| **No** Kafka | — | Overkill hasta multi-instalación federada |

Mantener Postgres como source of truth del estado de `Deployment`/`PipelineRun`. Redis es transporte/cache, no sistema de registro.

### Fase C — Workers horizontales

- N réplicas `atlas-worker` en la red `atlas-internal`.
- Idempotencia por `deployment_id` / `job_id`.
- Heartbeat + reclaim de locks expirados.
- Límites: max concurrent deploys por host y por instalación.

## Tipos de job (catálogo inicial)

| `type` | Payload mínimo | Side effects |
|--------|----------------|--------------|
| `DEPLOY_SERVICE` | deploymentId | git, compose, update status/logs |
| `STOP_SERVICE` | serviceId, hostId | compose down |
| `SYNC_HOST` | hostId | docker version, online, containers |
| `FETCH_LOGS_SNAPSHOT` | deploymentId | opcional; preferir stream |
| `BACKUP_VOLUME` | volumeId | archive + storage |
| `RENEW_CERTIFICATE` | certificateId | ACME / Traefik trigger |

## Observabilidad de workers

- Métricas: `atlas_jobs_pending`, `atlas_jobs_running`, `atlas_job_duration_seconds`, fallos por tipo.
- Logs estructurados con `jobId`, `deploymentId`, `projectId`.
- Dead-letter: `status=DEAD` tras N attempts + alerta.

## Anti-patrones

- Ejecutar `docker compose` dentro del request HTTP del controller.
- Cola sin idempotencia (doble deploy).
- Un worker monolítico sin timeout ni cancelación.
