# Deployment — Git webhooks vs Authentik edge

## Problema

GitHub (y clientes similares) **no siguen redirects** en deliveries de webhook.
Si `atlas.atlasops.dev` está detrás de Authentik ForwardAuth, un `POST` anónimo a
`/api/v1/webhooks/git/{token}` recibe **302** → Authentik login →
`Invalid HTTP Response: 302`.

Atlas API ya hace `permitAll` + auth por token en el path; el fallo es el **edge**.

## Fix (dos capas)

### 1. Traefik — router sin middleware `authentik` (canónico en Compose)

En `docker-compose.prod.yml` (partir de `docker-compose.prod.yml.example`):

| Router | Rule | Middlewares | Priority |
|--------|------|-------------|----------|
| `atlas` | `Host(atlas…)` | `authentik`, securityHeaders, gzip | 1 |
| `atlas-webhooks` | `Host(atlas…) && PathPrefix(/api/v1/webhooks/)` | securityHeaders, gzip (**sin** authentik) | 100 |

Ambos apuntan al mismo service `atlas` (frontend nginx → backend).

Tras editar labels: `docker compose -f docker-compose.prod.yml up -d frontend`
(o recreate del servicio frontend). Deploy script **no** pisa este fichero.

### 2. Authentik — `skip_path_regex` (defensa en profundidad)

Provider **Provider for Atlas** → Unauthenticated Paths / Excluded paths /
`skip_path_regex` (campo único; líneas = OR):

**Debe ser regex anclado**, no string suelto:

| Mal (string suelto) | Bien (regex) |
|---------------------|--------------|
| `/api/v1/webhooks/` | `^/api/v1/webhooks/` |

```text
^/api/v1/webhooks/
```

Conservar otras exclusiones necesarias (p. ej. `^/api/v1/auth/`) en líneas
aparte del mismo campo. Útil si alguien re-aplica middleware `authentik` en el
path de webhooks. **No** sustituye el router Traefik.

Tras cambiar: tocar outpost / `docker restart authentik-server authentik-worker`
para que Embedded Outpost recargue config.

Shell de ejemplo (VM):

```bash
docker exec -i authentik-server ak shell -c "
from authentik.providers.proxy.models import ProxyProvider
from authentik.outposts.models import Outpost
p = ProxyProvider.objects.get(name='Provider for Atlas')
p.skip_path_regex = r'^/api/v1/webhooks/'
p.save()
for o in Outpost.objects.filter(providers=p):
    o.save()
print('skip_path_regex=', repr(p.skip_path_regex))
"
docker restart authentik-server authentik-worker
```

## Verificar

Sin cookie / sin seguir redirects — **no** debe ser 302:

```bash
curl -sS -o /dev/null -w "%{http_code} redirect=%{redirect_url}\n" \
  --max-redirs 0 \
  -X POST "https://atlas.atlasops.dev/api/v1/webhooks/git/probe-token" \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: ping" \
  -d '{"zen":"edge-check"}'
```

Esperado: **404** (token inexistente), **401** (firma/secret), **204** (ping OK), etc.
**Nunca 302** a `auth.atlasops.dev`.

UI root sigue protegida:

```bash
curl -sSI --max-redirs 0 "https://atlas.atlasops.dev/" | head -5
# expect 302 → Authentik authorize
```

### GitHub Redeliver

1. Repo → **Settings → Webhooks** → hook cuya URL contiene `/api/v1/webhooks/git/`.
2. **Recent Deliveries** → delivery fallida → **Redeliver**.
3. Response code ≠ 302 (ideal 204/200 según evento). No borrar el proyecto en Atlas.

## Seguridad

- Path público solo bajo `/api/v1/webhooks/`; auth = token en URL + secret HMAC en API.
- No ampliar exclusión a `/api/v1/` entero.
- Backend sigue sin puerto público; tráfico webhook entra por Traefik → nginx → API.

## SSO bootstrap (`/api/v1/auth/sso/bootstrap`)

### Problema

El SPA necesita un `GET` de documento a `/api/v1/auth/sso/bootstrap` para que Traefik
ForwardAuth inyecte `X-authentik-*` y el backend escriba el JWT en `localStorage`.
Si el navegador abre **bootstrap directamente** sin sesión Authentik, ForwardAuth puede
responder **403** (“No tienes autorización…”) en lugar de dejar pasar la petición al
backend (que redirigiría a `/outpost.goauthentik.io/start`).

### Fix (dos capas)

| Capa | Qué |
|------|-----|
| **Frontend** | `redirectToSsoBootstrap()` en prod → `/outpost.goauthentik.io/start?rd=<bootstrap-url>` (nunca bootstrap a pelo). |
| **Traefik** | Bootstrap **permanece** en router `atlas` **con** middleware `authentik` (post-login necesita cabeceras). **No** crear router sin Authentik (rompe el mint JWT). |
| **Spring** | `permitAll` en `GET /api/v1/auth/sso/bootstrap` (fallback local / bookmark). |

### Flujo correcto

```text
SPA sin JWT
  → outpost start ?rd=bootstrap-url
  → Authentik login
  → GET bootstrap (ForwardAuth + headers)
  → HTML localStorage + redirect returnTo
  → SPA con Bearer JWT
```

### Verificar

```bash
# Inicio SSO: 302 a Authentik (no 403)
curl -sS -o /dev/null -w "%{http_code}\n" --max-redirs 0 \
  "https://atlas.atlasops.dev/outpost.goauthentik.io/start?rd=https%3A%2F%2Fatlas.atlasops.dev%2Fapi%2Fv1%2Fauth%2Fsso%2Fbootstrap%3FreturnTo%3D%252F"
# Esperado: 302

# Bootstrap a pelo sin cookie: 302 o 403 según sesión; el SPA no debe usar esta URL como entrypoint.
curl -sS -o /dev/null -w "%{http_code}\n" --max-redirs 0 \
  "https://atlas.atlasops.dev/api/v1/auth/sso/bootstrap?returnTo=%2F"
```
