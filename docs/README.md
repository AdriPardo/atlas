# Atlas — Documentación de plataforma

Atlas es una plataforma de ingeniería autoalojada de grado comercial: registra, despliega, observa y opera proyectos sobre infraestructura propia (Docker / Proxmox / Traefik / Authentik), inspirada en Vercel, Coolify, Railway, Portainer, GitHub Actions, Datadog y Grafana — **no** un juguete de homelab.

Esta carpeta describe el **diseño objetivo** y la evolución desde el MVP actual. **No sustituye al código**: el código es la fuente de verdad de lo implementado; estos docs son la fuente de verdad del producto.

## Cómo leer

| Orden | Documento | Para qué |
|------:|-----------|----------|
| 1 | [architecture/overview.md](architecture/overview.md) | Visión, capas, principios |
| 2 | [domain/model.md](domain/model.md) + [bounded-contexts.md](domain/bounded-contexts.md) | Entidades y límites |
| 3 | [modules/catalog.md](modules/catalog.md) | Inventario de módulos y estado |
| 4 | [api/conventions.md](api/conventions.md) + [endpoints.md](api/endpoints.md) | Contrato REST |
| 5 | [database/schema.md](database/schema.md) | Modelo de datos |
| 6 | [ux/information-architecture.md](ux/information-architecture.md) | Navegación y pantallas |
| 7 | [roadmap/versions.md](roadmap/versions.md) | v0.1 → v1.0 |
| 8 | [roadmap/next-step.md](roadmap/next-step.md) | Siguiente paso de implementación |
| — | [decisions/](decisions/) | ADRs (decisiones vinculantes) |
| — | [product/project-database-access.md](product/project-database-access.md) | Acceso DB por Project (ADR-0015) |

## Estado del producto (hoy)

| Concepto MVP | Estado | Evolución documentada |
|--------------|--------|------------------------|
| `Application` | CRUD + registro | → `Project` + `Service` ([ADR-0004](decisions/ADR-0004-application-to-project-evolution.md)) |
| `Host` | CRUD inventario | → Host + Agent + conectividad real |
| `Deployment` | Manual / simulado | → Pipeline + Job + ejecución real |
| `User` (`ADMIN`/`OPERATOR`) | JWT + Authentik SSO | → RBAC + Teams + Organizations (faseada) |
| Puertos `Unsupported*` | Stub | → adapters Docker/SSH/Git |

Instalación **single-tenant** por deploy de Atlas ([ADR-0002](decisions/ADR-0002-single-tenant-install.md)). Muchas instalaciones independientes; miles de proyectos **dentro** de una instalación.

## Stack de diseño (no inventar otro)

- Backend: Java 21, Spring Boot, hexagonal (`domain` / `application` / `infrastructure` / `bootstrap`)
- Frontend: React + TypeScript + Vite + TanStack Query + MUI
- Datos: PostgreSQL + Flyway
- Edge: Traefik + Cloudflare Tunnel + Authentik ForwardAuth
- Observabilidad externa: Prometheus, Grafana, Loki, Node Exporter (Atlas integra, no reinventa)

## Convenciones de esta documentación

- Prosa en **español**; identificadores, APIs y nombres de entidad en **inglés**.
- Estados de módulo: `MVP` · `designed` · `v0.x` · `v1.0` · `future`.
- Ningún archivo de este árbol implica código escrito: son contratos de diseño.
