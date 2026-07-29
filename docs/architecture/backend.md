# Arquitectura — Backend

## Forma actual (MVP)

Gradle multi-módulo hexagonal:

```text
backend/
  domain/           # Application, Host, Deployment, User — sin Spring/JPA
  application/      # use cases + ports (repos + Unsupported*: HostConnector, Git, ContainerRuntime)
  infrastructure/   # JPA/Flyway, JWT, BCrypt, AdminUserInitializer, adapters Unsupported*
  bootstrap/        # Controllers REST /api/v1, SecurityConfig, OpenAPI, Actuator
```

Esto se **conserva**. El crecimiento es por paquetes de bounded context dentro de `domain`/`application`, no por fragmentar en microservicios.

## Capas y reglas

| Capa | Puede depender de | Prohibido |
|------|-------------------|-----------|
| `domain` | JDK | Spring, JPA, HTTP |
| `application` | `domain` | Framework web, detalles infra |
| `infrastructure` | `application` + `domain` | Controllers |
| `bootstrap` | todo | Lógica de negocio |

Casos de uso: un comando/query por clase (`CreateXUseCase`, `ListXUseCase`). Orquestación corta; invariantes en dominio.

## Puertos actuales y destino

| Puerto | Hoy | Destino |
|--------|-----|---------|
| `*RepositoryPort` | JPA adapters | Igual; añadir filtros/paginación cursor |
| `TokenProviderPort` | JWT HS* | Igual; claims ricos (roles, team ids) |
| `PasswordEncoderPort` | BCrypt | Igual; usuarios SSO con hash inutilizable |
| `HostConnectorPort` | `Unsupported*` | SSH + health probe + docker context |
| `GitRepositoryPort` | `Unsupported*` | clone/fetch/webhook verify |
| `ContainerRuntimePort` | `Unsupported*` | compose up/down/ps/logs vía Docker API |
| *(nuevo)* `SecretStorePort` | — | Cifrado at-rest / integración Vault-like |
| *(nuevo)* `JobQueuePort` | — | Encolar/claim jobs |
| *(nuevo)* `EventPublisherPort` | — | Outbox → bus interno |
| *(nuevo)* `MetricsQueryPort` | — | Proxy a Prometheus |
| *(nuevo)* `LogsQueryPort` | — | Proxy a Loki |

## Perfiles Spring

| Profile | Uso |
|---------|-----|
| `local` | Dev sin Authentik; login password |
| `docker` | Compose; Authentik SSO on por defecto |
| `test` | Testcontainers |
| `worker` *(futuro)* | Solo consumidores de jobs; sin exponer REST completo |

## API bootstrap

- Prefijo: `/api/v1` ([ADR-0008](../decisions/ADR-0008-api-versioning.md))
- Controllers delgados → DTO ↔ use case
- Errores: `ApiError` global (código, mensaje, traceId)
- OpenAPI en local/docker/test
- Actuator: `health`, `info` (y métricas Micrometer en fases posteriores)

## Crecimiento modular (paquetes)

```text
domain/
  project/          # evoluciona desde application/
  service/
  deployment/
  host/
  pipeline/
  identity/         # user, team, role, permission
  secrets/
  networking/       # domain, certificate, route
  observability/    # alert rule (metadata; datos en Prometheus)
  audit/
  billing/          # usage records (diseño)
  shared/
```

Un módulo de producto ≠ un JAR. En v1.0 sigue siendo un monolito modular; los límites son paquetes + puertos.

## Transacciones

- Un use case = una transacción por defecto (`@Transactional` en application o adapters).
- Side-effects largos (deploy): el use case **persiste** `Deployment` + `Job` y retorna; el worker ejecuta fuera de la TX HTTP.
- Patrón outbox para eventos que deben publicarse de forma fiable (ver [events.md](events.md)).

## Concurrencia y locks

- Deploy exclusivos por `service_id` vía job uniqueness / advisory lock Postgres.
- Claims de job: `FOR UPDATE SKIP LOCKED` (fase temprana) o Redis Streams (fase carga).

## Testing

- Unit: domain + use cases con fakes de ports.
- Integration: Testcontainers Postgres (ya existe).
- Contract: OpenAPI snapshot en `docs/api/openapi.json` + test `OpenApiContractIntegrationTest` (v0.8.16).
