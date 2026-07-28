# ADR-0014 — Project manifest como fuente de verdad del runtime

- **Estado:** Accepted (dirección estratégica; motor aún no implementado)
- **Fecha:** 2026-07-27

## Contexto

Hoy el path de deploy acopla la intención del usuario a tecnologías concretas:

| Capa | Acoplamiento actual |
|------|---------------------|
| Modelo `Service` / `Project` | Campo obligatorio `composePath` (p. ej. `docker-compose.atlas.yml`) |
| Job `DEPLOY_SERVICE` | `GitRepositoryPort.cloneOrUpdate` → `ContainerRuntimePort.composeUp(...)` |
| `ContainerRuntimePort` | API nombrada `composeUp` / `composeDown`; adapter real ejecuta `docker compose` (LOCAL socket o SSH) |
| Host | `dockerVersion`, sync orientado a Docker |
| Edge Autopilot | Traefik labels + Cloudflare Tunnel + DNS CNAME (correcto como *platform* edge, no como “cómo arrancar la app”) |
| Producto / UI | “New Project = repo + compose path”; Autopilot placement asume compose en el host |

Eso funciona para v0.4–v0.8, pero ancla Atlas a Docker Compose. La visión de producto: **el operador describe qué debe correr**; Atlas (Autopilot) decide *dónde* y *cómo se expone*; el *runtime* es un adapter sustituible (Compose hoy; Podman / Kubernetes / systemd mañana) sin cambiar el mental model del usuario.

Hexagonal ya anticipa adapters ([ADR-0001](ADR-0001-hexagonal-architecture.md)); falta un **contrato de proyecto** independiente del motor.

## Decisión

1. **Fuente de verdad del “cómo correr”:** un manifiesto de proyecto en el repo (nombre canónico: `atlas.yml`; alias aceptado: `atlas.project.yml`). Atlas lo lee tras el clone; no sustituye Git ni secrets de plataforma.
2. **Runtime pluggable:** el use case de deploy habla un port de orquestación genérico (evolución de `ContainerRuntimePort` → p. ej. `RuntimeOrchestratorPort`: `apply` / `teardown` / `status` / `logs`). El adapter **Compose** es el default actual; otros runtimes son adapters futuros.
3. **Separación de ownership:**

   | Owns the project manifest | Owns Atlas Autopilot / control plane |
   |---------------------------|--------------------------------------|
   | Runtime *kind* + hint de archivo legado | Placement (SHARED / ISOLATED, Host, Proxmox) — [ADR-0010](ADR-0010-autopilot-placement.md), [ADR-0012](ADR-0012-autopilot-proxmox-provisioner.md) |
   | Services: build, image, command, ports internos, health | Exposure `PUBLIC` \| `INTERNAL` |
   | Env *keys* / bindings declarados (no valores secretos) | Secrets cifrados + resolución `git.token`, etc. |
   | Dependencias entre services del stack | Domain stub, Traefik metadata, Tunnel ingress ([ADR-0011](ADR-0011-autopilot-tunnel-ingress.md)), DNS CNAME ([ADR-0013](ADR-0013-autopilot-dns-cname.md)) |
   | Recursos tipados opcionales (cpu/mem) como *deseos* | Capacidad del host, scheduling, audit, jobs |

4. **Migración desde Compose path:**
   - **Fase A (ahora):** documentar contrato; deploys siguen con `composePath` + `composeUp`.
   - **Fase B:** si existe `atlas.yml`, el job lo parsea; si `runtime.kind: compose` (o omitido) y hay `runtime.composeFile` / legacy path, el adapter Compose sigue siendo el ejecutor.
   - **Fase C:** UI/API dejan de exigir `composePath`; campo DB pasa a opcional / derivado (`manifestPath` + snapshot). Repos solo-compose sin manifiesto: Atlas sintetiza un manifiesto mínimo (`kind: compose`, `composeFile: <composePath>`).
   - **Fase D:** renombrar/ampliar el port; Host deja de asumir Docker como única capacidad (tags `runtime=compose|podman|k8s|…`).

5. **No reescribir** Hosts / Deployments / Jobs ni Traefik/Tunnel en este ADR. Autopilot sigue siendo capa de política sobre el control plane existente.

## Esquema inicial (sketch YAML)

Ver también el ejemplo completo: [`docs/schemas/atlas.project.example.yml`](../schemas/atlas.project.example.yml).

```yaml
apiVersion: atlas/v1alpha1
kind: Project
metadata:
  name: my-app          # hint; Atlas Project name sigue en control plane
  # slug opcional

runtime:
  kind: compose         # compose | podman-compose | kubernetes | systemd | custom (futuro)
  # Solo para kind compose / podman-compose — puente de migración:
  composeFile: docker-compose.atlas.yml

services:
  api:
    build:
      context: ./api
      dockerfile: Dockerfile
    # image: ghcr.io/acme/api:stable   # alternativa a build
    expose:
      port: 8080
      protocol: http
    health:
      path: /actuator/health
      intervalSeconds: 15
    envFrom:
      - secretRef: db.password          # nombre lógico Atlas Secrets
      - configKey: DOMAIN               # relleno por Autopilot / .env seed

  web:
    image: ghcr.io/acme/web:latest
    expose:
      port: 80
      protocol: http
    dependsOn: [api]

# Opcional: intención de edge (Autopilot puede override / enriquecer)
exposure:
  default: public       # public | internal — UI Autopilot sigue siendo autoridad en deploy
```

Reglas de diseño del schema:

- **Mínimo viable:** `apiVersion` + `runtime.kind` (+ `composeFile` mientras el motor sea Compose).
- `services.*` es la proyección *deseada*; el adapter Compose puede mapear 1:1 a un compose existente **sin** exigir duplicar el compose en YAML si solo se declara `runtime.composeFile`.
- Valores secretos **nunca** en el manifiesto; solo refs.
- Campos Traefik/Tunnel/DNS **no** viven aquí como labels crudas; Autopilot las deriva de `expose` + Domain + exposure.

## Consecuencias

- (+) Usuario y repo dejan de “hablar Docker”; Compose es un detalle de adapter.
- (+) Alineado con hexagonal: un port, N runtimes.
- (+) Migración barata: `composePath` → `runtime.composeFile` sin big-bang.
- (−) Hasta Fase B el código sigue acoplado a `composeUp` / `composePath`.
- (−) Riesgo de duplicar verdad (manifiesto vs compose file): mitigar con “composeFile only” como modo válido en v1alpha1.
- (−) K8s/systemd no se diseñan en detalle aquí; solo se reserva `runtime.kind`.

## Qué no hacer aún

- No implementar motor completo de manifiesto ni parsers en el hot path de deploy.
- No eliminar `composePath` de API/DB en este incremento.
- No forzar a todos los customer repos a adoptar `atlas.yml` antes de Fase C.
- No meter billing/AI ni rewrite de Hosts/Deployments.
- No añadir un segundo port paralelo vacío sin consumidores; evolucionar `ContainerRuntimePort` cuando exista el primer parser.

## Relación con el siguiente paso operativo

El runbook restore v0.8 está cerrado (`docs/deployment/backup-restore.md`). Este ADR sigue siendo **norte estratégico** / slice posterior (tras polish Autopilot `REUSED` / cuando toque desacoplar `composePath`); ver `docs/roadmap/next-step.md`.
