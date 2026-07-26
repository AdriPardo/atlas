# Atlas

Plataforma de operaciones para aplicaciones autoalojadas.

Atlas **gestiona** aplicaciones (registro, inventario, despliegues futuros). No las ejecuta en su propio runtime.

## Stack MVP

| Capa | Tecnología |
| --- | --- |
| Backend | Java 21, Spring Boot 3, Gradle, PostgreSQL, Flyway, Security JWT, MapStruct, OpenAPI |
| Frontend | React, TypeScript, Vite, TanStack Query, MUI, React Hook Form, Zod, Axios |
| Arquitectura | Hexagonal (Domain / Application / Infrastructure / API) |

## Arranque con Docker Compose

```bash
docker compose up --build
```

- UI: http://localhost:3000
- API: http://localhost:8080
- OpenAPI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

### Credenciales seed

| Usuario | Password | Rol |
| --- | --- | --- |
| `admin` | `admin123` | ADMIN |
| `operator` | `operator123` | OPERATOR |

## Desarrollo local

### Backend

```bash
# Postgres en marcha (compose solo DB o local)
cd backend
./gradlew bootRun
```

### Frontend

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

## Estructura

```text
backend/     Control plane API (hexagonal)
frontend/    SPA operativa
docs/architecture/saas-mvp.md
docker-compose.yml
```

## MVP incluido

- Login JWT + roles ADMIN / OPERATOR
- CRUD de Applications (paginación, orden, filtros)
- Listado/detalle de Hosts
- Listado/detalle de Deployments (sin ejecución real)
- Dashboard y perfil
- Puertos preparados (sin implementación): `DeploymentExecutorPort`, `HostConnectorPort`, `GitClientPort`, `MetricsPort`

## Fuera de alcance (intencional)

SSH, Docker Engine, Git clone/pull, despliegues reales, Prometheus/Grafana/Loki, Cloudflare, Kubernetes/Swarm, CI/CD.

## Tests backend

```bash
cd backend
./gradlew test
```

## Nota sobre skills Reelpath

En este entorno cloud no estaban disponibles skills del proyecto Reelpath. La implementación sigue arquitectura hexagonal y estándares Spring/React del briefing Atlas.
