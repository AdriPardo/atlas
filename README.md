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
| Auth (prod) | Authentik ForwardAuth → Atlas JWT (SSO); password login only for local/dev |

Arquitectura hexagonal:

```
backend/
  domain/           # modelos de dominio (sin Spring/JPA)
  application/      # casos de uso + puertos
  infrastructure/   # JPA, JWT, adapters (incl. Unsupported* futuros)
  bootstrap/        # API REST, Security, OpenAPI, Actuator
frontend/           # SPA
```

## Autenticación

### Producción (detrás de Authentik + Traefik)

Traefik ForwardAuth inyecta cabeceras (`X-authentik-username`, `X-authentik-groups`,
`X-authentik-email`, `X-authentik-name`, `X-authentik-uid`, …). Atlas:

1. Expone `GET/POST /api/v1/auth/sso` (permitAll).
2. Si `atlas.security.authentik.enabled=true` (perfil `docker` por defecto), confía en esas cabeceras.
3. Auto-provisiona el usuario en DB (hash de password inutilizable) y emite un **JWT Atlas** (mismo contrato que el login local).
4. El SPA, al arrancar, llama a `/auth/sso`; si responde 200, guarda el token y entra al dashboard **sin** mostrar el formulario de login.

**Mapeo de grupos → roles**

| Authentik (`X-authentik-groups`, separados por `\|`) | Rol Atlas |
|---|---|
| Algún grupo cuyo nombre contiene `Atlas Admins` (configurable vía `ATLAS_AUTHENTIK_ADMIN_GROUP`, case-insensitive) | `ADMIN` |
| Cualquier otro / sin grupos | `OPERATOR` |

**Limitación de seguridad:** confiar en cabeceras solo es seguro si Atlas no es alcanzable sin Traefik ForwardAuth. No expongas el puerto del backend a redes no confiables con SSO activado (un cliente podría falsificar `X-authentik-*`).

### Local / desarrollo (sin Authentik)

Con `ATLAS_AUTHENTIK_ENABLED=false` (perfil `local` por defecto) o sin cabeceras Authentik, `/auth/sso` responde 401 y el SPA muestra el login usuario/password JWT habitual (`admin` / `ChangeMe123!`).

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

- Login JWT local + SSO Authentik ForwardAuth (roles `ADMIN`, `OPERATOR`)
- CRUD Applications / Hosts / Deployments
- **Deploy real**: `POST /applications/{id}/deploy` → job `DEPLOY_SERVICE` → Git clone + `docker compose up`
- **Sync host**: `POST /hosts/{id}/sync` → job `SYNC_HOST`
- Cola Postgres (`jobs`, `SKIP LOCKED`) + worker embebido (`ATLAS_WORKER_ENABLED`)
- Secrets cifrados AES (`ATLAS_SECRETS_MASTER_KEY`); hosts `LOCAL` | `SSH`
- Dashboard, perfil, paginación, seed admin, auto-provision SSO

## Desplegar una app desde la UI (LOCAL)

1. Arranca stack (`docker compose up --build`). Para LOCAL deploys desde el contenedor, descomenta el mount de `/var/run/docker.sock` en `docker-compose.yml` (solo en installs de confianza).
2. Crea un **Host** con `connectionType=LOCAL`, IP `127.0.0.1`, OS placeholder (p.ej. `linux`).
3. Pulsa **Sync** en el detalle del host (rellena Docker/OS si el socket/CLI están reachable).
4. Crea una **Application** apuntando a un repo **público** con `docker-compose.yml` (o crea secret `git.token` vía `POST /api/v1/secrets` para privados).
5. En el detalle de la app → **Deploy** → elige el host → se abre el deployment con logs en vivo.
6. Criterio de éxito: status `SUCCEEDED` o `FAILED` con logs honestos.

**SSH:** crea un secret con la private key PEM, asócialo al host (`sshPrivateKeySecretId`), `connectionType=SSH`, user/port/IP correctos. Sin secret, el deploy falla de forma explícita.

## Desarrollo local sin Compose backend

`./gradlew :bootstrap:bootRun` en el host con Docker instalado usa el engine local para hosts `LOCAL` (sin montar socket).

### Separar worker más adelante

Misma imagen jar: API con `ATLAS_WORKER_ENABLED=false`, replica worker con `true`. Ver [ADR-0009](docs/decisions/ADR-0009-embedded-worker.md).

## Preparado / extensible

Adapters reales detrás de puertos hexagonales; stubs `Unsupported*` si `ATLAS_ADAPTERS_REAL_ENABLED=false`.

No hay todavía: Project/Service rename, Prometheus/Grafana/Loki first-class, Kubernetes, multi-tenant.

## Documentación de arquitectura y producto

Diseño objetivo (plataforma comercial self-hosted), dominio, API, UX, schema, ADRs y roadmap:

→ **[docs/README.md](docs/README.md)**

Siguiente paso recomendado: [docs/roadmap/next-step.md](docs/roadmap/next-step.md).

## API

Base: `/api/v1`

| Recurso | Rutas |
|---------|--------|
| Auth | `POST /auth/login`, `GET/POST /auth/sso` |
| Me | `GET /me`, `GET /dashboard/stats` |
| Applications | `GET/POST /applications`, `GET/PUT/DELETE /applications/{id}`, `POST /applications/{id}/deploy` |
| Hosts | `GET/POST /hosts`, `GET/PUT/DELETE /hosts/{id}`, `POST /hosts/{id}/sync` |
| Deployments | `GET/POST /deployments`, `GET/PUT/DELETE /deployments/{id}` |
| Jobs | `GET /jobs`, `GET /jobs/{id}` |
| Secrets | `GET/POST /secrets` (valores nunca se listan) |

Autenticación: header `Authorization: Bearer <token>` (emitido por `/login` o `/sso`).

## Variables de entorno

Ver [`.env.example`](.env.example):

- `ATLAS_DB_*`
- `ATLAS_JWT_SECRET` (mín. 32 caracteres)
- `ATLAS_JWT_EXPIRATION`
- `ATLAS_CORS_ORIGINS`
- `ATLAS_ADMIN_USERNAME` / `ATLAS_ADMIN_PASSWORD`
- `ATLAS_AUTHENTIK_ENABLED` / `ATLAS_AUTHENTIK_ADMIN_GROUP`
- `ATLAS_SECRETS_MASTER_KEY`
- `ATLAS_WORKER_ENABLED` / `ATLAS_WORKSPACE_DIR` / `ATLAS_DOCKER_HOST`
- `ATLAS_ADAPTERS_REAL_ENABLED`
- `SPRING_PROFILES_ACTIVE` (`local` \| `docker` \| `test`)

## IDE

- Backend: abrir el directorio `backend/` en IntelliJ como proyecto Gradle.
- Frontend: abrir `frontend/` en VS Code / Cursor.
