# Arquitectura — Gateway y edge

## Topología existente (realidad a respetar)

```text
Internet
   │
Cloudflare Tunnel
   │
Traefik (entrada HTTPS, routers, middlewares)
   │
   ├── ForwardAuth → Authentik
   │
   ├── atlas-ui (frontend nginx) ──proxy /api──► atlas-backend
   ├── grafana / prometheus / … (stack ops)
   └── otras apps del cliente
```

Redes Docker externas usadas por Compose de Atlas:

- `atlas-public` — UI alcanzable vía Traefik
- `atlas-internal` — Postgres, backend, workers

## Responsabilidades por capa

| Componente | Hace | No hace |
|------------|------|---------|
| Cloudflare Tunnel | Exposición segura sin IP pública | Auth de aplicación |
| Traefik | TLS (o tunnel), routing, ForwardAuth, rate limit | Lógica de negocio Atlas |
| Authentik | IdP SSO, grupos | Autorización fina de proyectos (Atlas) |
| nginx (frontend) | SPA + proxy `/api` + forward headers Authentik | Auth propia |
| Atlas API | JWT, RBAC, dominio | Terminar TLS público |

## Headers Authentik (contrato)

Reenviados por nginx a backend (ya en `docker/nginx.conf`):

- `X-authentik-username`, `X-authentik-groups`, `X-authentik-email`
- `X-authentik-name`, `X-authentik-uid`, `X-authentik-jwt` (opcional)

Solo confiables si el backend **no** es reachable sin el middleware.

## Dominios y certificados (módulo futuro)

Atlas modela `Domain`, `Certificate`, `DnsRecord` y emite configuración deseada hacia:

- Traefik (labels / file provider / API), y/o
- Cloudflare (API DNS / SSL modes).

Atlas es **control plane** de intención; Traefik/Cloudflare son **data plane**.

## Routing de aplicaciones gestionadas

Cuando un `Service` declara `domain`:

1. Atlas valida ownership DNS (TXT/CNAME challenge).
2. Job aplica router Traefik → contenedor en host target.
3. Estado reflejado en UI (Domains module).

## Health y readiness en el gateway

- Traefik healthcheck → frontend `/` o backend `/actuator/health`.
- No exponer Swagger en producción pública (restringir por perfil / IP allowlist).

## Diagrama de confianza

```text
[Usuario] → Cloudflare → Traefik+Authentik → [UI]
                                      └→ [API]  (solo red interna + headers)
[API] → Postgres / Redis
[Worker] → Hosts (SSH/Docker)   // credenciales nunca al browser
```
