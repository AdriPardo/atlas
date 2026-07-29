# Arquitectura — Seguridad

## Modelo de amenaza (instalación self-hosted)

| Activo | Riesgo | Mitigación |
|--------|--------|------------|
| API Atlas | Acceso no autorizado | JWT + Authentik en edge; red interna |
| Secretos de proyectos | Exfiltración | Cifrado at-rest, ACL, audit |
| Hosts Docker | Ejecución remota abusiva | Credenciales por host, least privilege, audit |
| Cabeceras Authentik | Spoofing si backend expuesto | **No** publicar puerto API sin Traefik ForwardAuth |
| JWT | Robo / replay | HTTPS, expiración corta, rotación secret |

## Autenticación

### Producción

1. Usuario autentica en Authentik.
2. Traefik ForwardAuth inyecta `X-authentik-*`.
3. SPA → `GET /api/v1/auth/sso` → Atlas provisiona/actualiza `User` y emite **JWT Atlas**.
4. API exige `Authorization: Bearer`.

Ver [ADR-0003](../decisions/ADR-0003-authentik-sso.md).

### Desarrollo

`ATLAS_AUTHENTIK_ENABLED=false` → login password local (`POST /auth/login`).

### Roles actuales → evolución

| Hoy | Evolución |
|-----|-----------|
| `ADMIN` | Superusuario de la instalación |
| `OPERATOR` | Operador de proyectos/deploys |
| — | `VIEWER`, `DEVELOPER`, roles custom vía permissions (v0.7+) |

Mapeo Authentik: grupos → roles (`ATLAS_AUTHENTIK_ADMIN_GROUP`, etc.).

## Autorización

- v0.x: `@PreAuthorize` por rol en endpoints mutantes sensibles.
- v0.7+: RBAC resource-scoped (`project:{id}:deploy`), Teams como subject groups.
- Organization: entidad única implícita por instalación; tabla `organizations` preparada para metadata y futura federación — **sin** aislamiento multi-tenant SaaS.

## Secretos y variables

| Tipo | Almacenamiento | Visibilidad API |
|------|----------------|-----------------|
| Variable (no secreta) | Postgres texto | Lectura según ACL |
| Secret (org o project) | Ciphertext + `project_id` nullable | Nunca en claro en listados; reveal auditado |
| Binding | `project_secret_bindings` | Alias lógico → secret global |
| Credencial host (SSH key) | Secret store (por id) | Solo worker en memoria |

Resolución por nombre en deploy: binding alias → project-owned → org/global. Clave maestra: env / file mount (`ATLAS_SECRETS_MASTER_KEY`).

DB de apps: secret lógico `db.url` (schema `app_<slug>`); DB control plane `atlas` no se expone a projects — [ADR-0015](../decisions/ADR-0015-project-database-access.md).


## Hardening operativo

- Backend solo en `atlas-internal`; UI en `atlas-public` vía Traefik.
- CORS explícito (`ATLAS_CORS_ORIGINS`).
- CSRF off (stateless JWT) — OK detrás del mismo sitio / Bearer.
- Rate limit en Traefik para `/auth/login`.
- Audit log append-only para acciones privilegiadas.
- Edge: excluir solo `/api/v1/webhooks/` de Authentik (GitHub no sigue 302); ver [webhooks-edge.md](../deployment/webhooks-edge.md).

## Cumplimiento ligero

- Retention configurable de audit y deployment logs.
- Export de audit (JSON) para clientes enterprise (v1.0).
- Billing module registra *usage* (minutos de deploy, GB backup) sin requerir cobro.
