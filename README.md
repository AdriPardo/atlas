# Atlas

Plataforma de operaciones para aplicaciones autoalojadas.

Atlas no ejecuta las aplicaciones: las registra, administra y prepara el camino para desplegarlas en hosts controlados desde un panel único.

Cada instalación de Atlas es **single-tenant** (una organización por despliegue). El producto está pensado para muchas instalaciones independientes.

## Stack

| Capa | Tecnología |
|------|------------|
| Backend | Java 21, Spring Boot 3.4, Gradle multi-módulo, PostgreSQL, Flyway, JWT |
| Frontend | React, TypeScript, Vite, TanStack Query, MUI, React Hook Form, Zod |
| Ops | Docker, Docker Compose |

Arquitectura hexagonal:

```
backend/
  domain/           # modelos de dominio (sin Spring/JPA)
  application/      # casos de uso + puertos
  infrastructure/   # JPA, JWT, adapters (incl. Unsupported* futuros)
  bootstrap/        # API REST, Security, OpenAPI, Actuator
frontend/           # SPA
```

## Requisitos

- Java 21+
- Node.js 22+
- Docker / Docker Compose

## Arranque rápido (Docker Compose)

```bash
cp .env.example .env
docker compose up --build
```

- UI: http://localhost:3000
- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

Credenciales por defecto (definidas en `.env`):

- Usuario: `admin`
- Password: `ChangeMe123!`

Al arrancar, el backend crea el admin si no existe y **reasigna su password** si
`ATLAS_ADMIN_PASSWORD` no coincide con el hash guardado (útil tras cambiar `.env`).

Si el puerto host `5432` ya está ocupado, arranca con
`ATLAS_DB_HOST_PORT=5434 docker compose up --build` (el backend sigue usando
`postgres:5432` dentro de la red de Compose).

## Desarrollo local

### Base de datos

```bash
docker compose up postgres -d
```

### Backend

```bash
cd backend
./gradlew :bootstrap:bootRun
```

Perfil por defecto: `local` (`application.yml`).

### Frontend

```bash
cd frontend
npm install
npm run dev
```

UI de desarrollo: http://localhost:5173 (proxy `/api` → `8080`).

### Tests

```bash
cd backend
./gradlew test
```

Los tests de integración usan Testcontainers (PostgreSQL).

## MVP incluido

- Login JWT (roles `ADMIN`, `OPERATOR`)
- CRUD Applications
- CRUD Hosts
- CRUD Deployments (registro manual / estado simulado)
- Dashboard con contadores
- Perfil de usuario
- Paginación, ordenación y filtros en listados
- Manejo global de errores API
- Seed del usuario administrador

## Preparado, no implementado

Puertos en `application` con adapters `Unsupported*` en infrastructure:

- `HostConnectorPort` (SSH / conectividad remota)
- `GitRepositoryPort` (clone / pull)
- `ContainerRuntimePort` (docker compose up/down/logs)

No hay ejecución real de despliegues, Prometheus, Grafana, Loki, Kubernetes ni multi-tenant.

## API

Base: `/api/v1`

| Recurso | Rutas |
|---------|--------|
| Auth | `POST /auth/login` |
| Me | `GET /me`, `GET /dashboard/stats` |
| Applications | `GET/POST /applications`, `GET/PUT/DELETE /applications/{id}` |
| Hosts | `GET/POST /hosts`, `GET/PUT/DELETE /hosts/{id}` |
| Deployments | `GET/POST /deployments`, `GET/PUT/DELETE /deployments/{id}` |

Autenticación: header `Authorization: Bearer <token>`.

## Variables de entorno

Ver [`.env.example`](.env.example):

- `ATLAS_DB_*`
- `ATLAS_JWT_SECRET` (mín. 32 caracteres)
- `ATLAS_JWT_EXPIRATION`
- `ATLAS_CORS_ORIGINS`
- `ATLAS_ADMIN_USERNAME` / `ATLAS_ADMIN_PASSWORD`
- `SPRING_PROFILES_ACTIVE` (`local` \| `docker` \| `test`)

## IDE

- Backend: abrir el directorio `backend/` en IntelliJ como proyecto Gradle.
- Frontend: abrir `frontend/` en VS Code / Cursor.
