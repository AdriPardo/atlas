# Dominio — Bounded contexts

## Contextos

| Contexto | Lenguaje | Owns | Integra con |
|----------|----------|------|-------------|
| **Identity** | User, Team, Permission, Session/JWT | AuthN/Z, SSO provision | Todos (actor) |
| **Catalog** | Project, Service, Repository | Inventario de software | Delivery, Config |
| **Delivery** | Pipeline, Deployment, Job | Releases | Catalog, Runtime, Events |
| **Runtime** | Host, Container, Volume | Flota Docker | Delivery, Observability |
| **Config** | Variable, Secret, Environment | Config inyectable | Delivery |
| **Networking** | Domain, Cert, DNS, TraefikRoute | Edge desired state | Catalog, Gateway externo |
| **Observability** | Alert, Notification, LogQuery | Ops producto | Delivery, Runtime, stack externo |
| **Audit & Billing** | AuditEntry, UsageRecord | Compliance / metering | Todos (listeners) |
| **Platform** | Settings, Plugin | Instalación | Todos |

## Mapa de contexto (C4 ligero)

```text
[Identity] ──autoriza──► [Catalog] ──dispara──► [Delivery] ──ejecuta en──► [Runtime]
                              │                      │
                              └──► [Config] ◄─────────┘
                              └──► [Networking]
[Delivery/Runtime] ──emiten──► [Observability] + [Audit & Billing]
[Platform] configura providers de todos
```

## Reglas de acoplamiento

- Delivery **no** habla Docker directamente: usa `ContainerRuntimePort` / jobs.
- Observability **no** posee time-series: consulta adapters.
- Networking escribe intención; Traefik/Cloudflare adapters aplican.
- Billing solo escucha eventos / meters; no bloquea deploy en v0.x (soft limits opcionales en v1).

## Traducción anti-corruption

| Sistema externo | Adapter | Modelo Atlas |
|-----------------|---------|--------------|
| Authentik headers | SSO use case | User + Role |
| Docker API | ContainerRuntimePort | Container, Volume |
| Git | GitRepositoryPort | commit sha, tree |
| Prometheus | MetricsQueryPort | MetricSeries DTO |
| Loki | LogsQueryPort | LogLine DTO |
| Cloudflare API | DnsProviderPort | DnsRecord |
| Traefik | IngressPort | TraefikRoute |

## Por qué no más contextos

Evitar “micro-contexts” (p.ej. separar Cron de Platform) hasta que un equipo/módulo lo justifique. Preferir paquetes claros dentro del monolito.
