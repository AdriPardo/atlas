# Arquitectura — Overview

## Propósito

Atlas es el **panel de control** de una organización sobre su propia flota: proyectos, hosts Docker, despliegues, redes, secretos, logs y métricas. No es un PaaS multi-tenant SaaS en v0.x–v1.0; cada instalación sirve a **una** organización ([ADR-0002](../decisions/ADR-0002-single-tenant-install.md)).

## Principios

1. **Hexagonal + DDD pragmático** — dominio puro; adapters en infrastructure; casos de uso delgados ([ADR-0001](../decisions/ADR-0001-hexagonal-architecture.md)).
2. **Evolución sin big-bang** — renombrar/enriquecer MVP (`Application` → `Project`/`Service`) con migraciones y aliases API ([ADR-0004](../decisions/ADR-0004-application-to-project-evolution.md)).
3. **Workers solo donde pagan** — builds, deploys, sync de hosts; no un bus enterprise por defecto ([ADR-0005](../decisions/ADR-0005-workers-and-job-queue.md)).
4. **Eventos en fronteras caras** — ciclo de vida de deploy, alertas, audit; no CRUD event-sourcing ([ADR-0006](../decisions/ADR-0006-event-driven-boundaries.md)).
5. **Observabilidad por integración** — Prometheus/Grafana/Loki ya existen; Atlas enlaza y orquesta ([ADR-0007](../decisions/ADR-0007-observability-external-stack.md)).
6. **Seguridad por perímetro + JWT** — Authentik ForwardAuth en edge; JWT Atlas en API ([ADR-0003](../decisions/ADR-0003-authentik-sso.md)).

## Vista lógica

```text
┌─────────────────────────────────────────────────────────────────┐
│  Browser (SPA React)                                            │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTPS
┌────────────────────────────▼────────────────────────────────────┐
│  Cloudflare Tunnel → Traefik → Authentik ForwardAuth            │
│  (gateway / edge — ver gateway.md)                              │
└────────────────────────────┬────────────────────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        ▼                    ▼                    ▼
┌───────────────┐   ┌────────────────┐   ┌────────────────────┐
│ Atlas UI      │   │ Atlas API      │   │ Atlas Worker(s)    │
│ (nginx+SPA)   │   │ Spring Boot    │   │ jobs: deploy/build │
└───────────────┘   │ hexagonal      │   └─────────┬──────────┘
                    └───────┬────────┘             │
                            │                      │
                    ┌───────▼──────────────────────▼──┐
                    │ PostgreSQL (source of truth)    │
                    │ Redis (cache + opcional queue)  │
                    └─────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
   Hosts Docker/SSH    Git remotes         Prometheus /
   (via connectors)    (via Git port)      Grafana / Loki
```

## Capas del monorepo

| Capa | Ubicación | Responsabilidad |
|------|-----------|-----------------|
| Domain | `backend/domain` | Entidades, value objects, invariantes |
| Application | `backend/application` | Use cases + ports |
| Infrastructure | `backend/infrastructure` | JPA, JWT, Docker/SSH/Git adapters, jobs |
| Bootstrap | `backend/bootstrap` | REST, Security, OpenAPI, Actuator |
| Worker | (módulo futuro o mismo JAR con perfil `worker`) | Consumidores de jobs |
| Frontend | `frontend/` | SPA |
| Ops | `docker/`, compose, Traefik externo | Runtime |

## Escalabilidad objetivo

| Dimensión | Target de diseño |
|-----------|------------------|
| Instalaciones Atlas | Muchas, independientes |
| Proyectos por instalación | Miles |
| Concurrent deploys | Decenas → cientos (workers horizontales) |
| Usuarios por instalación | Decenas → cientos (Teams/RBAC) |
| Multi-tenant SaaS | **Fuera de alcance** hasta post-v1.0 |

Patrones: paginación cursor/offset, índices compuestos, soft tenancy via `organization_id` nullable preparado, partición lógica por `project_id`, jobs con `SKIP LOCKED`.

## Mapa de documentos de arquitectura

| Doc | Contenido |
|-----|-----------|
| [backend.md](backend.md) | Módulos Java, puertos, perfiles |
| [frontend.md](frontend.md) | SPA, features, estado |
| [workers-queues.md](workers-queues.md) | Jobs, Redis vs Postgres |
| [events.md](events.md) | Dominio de eventos |
| [security.md](security.md) | AuthZ/AuthN, secretos |
| [observability.md](observability.md) | Métricas, logs, traces |
| [gateway.md](gateway.md) | Traefik, Cloudflare, routing |

## Anti-objetivos (v0–v1)

- No reinventar K8s control plane.
- No embebido de Grafana/Prometheus como producto primario.
- No Kafka “por si acaso”.
- No microservicios prematuros: monolito modular + workers.
- No billing de cobro real obligatorio; el módulo existe como diseño.
