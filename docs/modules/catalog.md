# Catálogo de módulos

Estado: `MVP` (en código) · `v0.x` (roadmap) · `v1.0` · `future` · `designed` (especificado aquí).

## Inventario

| Módulo | Propósito | Estado | Doc |
|--------|-----------|--------|-----|
| Dashboard | Resumen salud, contadores, actividad reciente | MVP → enriquecer | [core-delivery.md](core-delivery.md) |
| Projects | Agrupa servicios, repo, entorno | designed (desde Application) | [core-delivery.md](core-delivery.md) |
| Services | Unidad desplegable (compose service / app) | designed | [core-delivery.md](core-delivery.md) |
| Deployments | Ejecuciones de release a un host | MVP (manual) → real | [core-delivery.md](core-delivery.md) |
| Repositories | Conexiones Git, webhooks | designed | [core-delivery.md](core-delivery.md) |
| Pipelines | Build → test → deploy declarativo | designed | [core-delivery.md](core-delivery.md) |
| Hosts | Inventario y conectividad de máquinas | MVP | [runtime.md](runtime.md) |
| Containers | Vista runtime Docker en hosts | designed | [runtime.md](runtime.md) |
| Storage / Volumes | Volúmenes y mounts | designed | [runtime.md](runtime.md) |
| Databases | Catálogo DB gestionadas / externas | designed (ADR-0015 contrato + `db.url`; provisioner post-billing) | [runtime.md](runtime.md) · [project-database-access.md](../product/project-database-access.md) |
| Queues | Registro de brokers (Redis/Rabbit) del cliente | designed | [runtime.md](runtime.md) |
| Cron | Jobs programados en flota | designed | [runtime.md](runtime.md) |
| Logs | Consulta/stream logs app y deploy | designed | [observability-modules.md](observability-modules.md) |
| Metrics | Series y paneles deep-link | designed | [observability-modules.md](observability-modules.md) |
| Alerts | Reglas producto + estado | designed | [observability-modules.md](observability-modules.md) |
| Notifications | Email/Slack/Webhook destinos | designed | [observability-modules.md](observability-modules.md) |
| Domains | Hostnames de servicios | v0.7 | [networking.md](networking.md) |
| Certificates | TLS metadata / renovación | v0.7 (metadata en Domain) | [networking.md](networking.md) |
| DNS | Records deseados | designed (TXT challenge en Domain) | [networking.md](networking.md) |
| Cloudflare | Provider DNS/Tunnel API | designed (stub) | [networking.md](networking.md) |
| Traefik | Rutas / middlewares deseados | v0.7 (labels metadata) | [networking.md](networking.md) |
| Secrets | Secretos cifrados | designed | [config-security.md](config-security.md) |
| Variables | Env config no secreta | designed | [config-security.md](config-security.md) |
| Users | Identidades locales/SSO | MVP | [identity.md](identity.md) |
| Teams | Grupos de acceso | designed | [identity.md](identity.md) |
| Organizations | Metadata instalación / futuro soft-tenancy | designed | [identity.md](identity.md) |
| Permissions | RBAC | designed | [identity.md](identity.md) |
| Audit | Trail de acciones | designed | [identity.md](identity.md) |
| Backups / Restore | Snapshots volúmenes/DB | designed | [data-protection.md](data-protection.md) |
| Plugins | Extensiones adapter | future | [platform.md](platform.md) |
| Marketplace | Catálogo de templates/plugins | future | [platform.md](platform.md) |
| Settings | Config instalación | designed | [platform.md](platform.md) |
| Billing | Usage metering (aunque precio=0) | v0.9 (meters + UI; sin Stripe) | [platform.md](platform.md) |
| AI Assistant | Ayuda operativa contextual | future | [platform.md](platform.md) |

## Evolución de nombres MVP → plataforma

| MVP | Plataforma | Notas |
|-----|------------|-------|
| Application | Project (+ Service) | Application monolítica ≈ Project con 1 Service |
| Host | Host | Se añade Agent/Connector |
| Deployment | Deployment | Se liga a PipelineRun opcional |
| User | User | + memberships |

Compatibilidad: alias API `/applications` deprecated hasta Sunset **2027-08-01** ([deprecations.md](../api/deprecations.md)); canónico = Projects + Services.
