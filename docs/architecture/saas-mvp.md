# Atlas SaaS — Arquitectura MVP

## Visión

Atlas gestiona aplicaciones autoalojadas; **no las ejecuta**. El control plane (este producto) orquesta registros, inventario y (futuro) despliegues sobre hosts remotos.

## Principios

1. Hexagonal (puertos y adaptadores): el dominio no depende de Spring/JPA.
2. Un caso de uso = una clase de aplicación.
3. Multiinstancia preparada (`installation_id`) con un único tenant por defecto en MVP.
4. Capabilidades futuras detrás de puertos (`DeploymentExecutorPort`, `HostConnectorPort`, `GitClientPort`, `MetricsPort`) sin implementación operativa.

## Módulos lógicos

```text
api            → REST, DTO, OpenAPI, errores
application    → casos de uso
domain         → modelos, puertos de salida, excepciones
infrastructure → JPA, JWT, Flyway, seed, adaptadores
```

## MVP entregado

- Auth JWT (ADMIN, OPERATOR)
- CRUD Applications (paginación, orden, filtros)
- Listado/detalle Hosts
- Listado Deployments (sin ejecución real)
- Perfil de usuario
- Frontend SPA (claro/oscuro)

## Fuera de MVP (solo puertos)

SSH, Docker Engine, Git, despliegue real, Prometheus/Grafana/Loki, Cloudflare, K8s/Swarm, CI/CD.
