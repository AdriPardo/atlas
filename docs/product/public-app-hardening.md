# Producto — Endurecimiento de apps PUBLIC

Garantías de plataforma cuando un Service se publica al mundo (`exposure: PUBLIC`). Decisión: [ADR-0016](../decisions/ADR-0016-public-app-hardening.md).

## Qué significa “encriptar APIs”

**TLS en tránsito (HTTPS).** El cliente llega por Cloudflare Tunnel (HTTPS) → Traefik `websecure` con TLS. No es cifrado de body aparte de TLS. Secrets de Atlas siguen cifrados at-rest (AES); eso es orthogonal.

| Garantía | Cómo |
|----------|------|
| Edge PUBLIC HTTPS | Domain stub + Traefik labels `tls=true` / `websecure` |
| Tunnel | Public Hostname tipo HTTPS → `traefik:443` |
| DNS | CNAME proxied (nube termina TLS al visitante) |

`INTERNAL` puede quedar en red privada sin TLS de mundo.

## Qué significa “minificar fronts”

Build de **producción** del SPA/static (Vite/Webpack/etc. con minify). Atlas no ejecuta el bundler por su cuenta: inyecta / asegura `NODE_ENV=production` en el `.env` del workspace de deploy cuando `build.minify` no es `false`. El `Dockerfile` / script de build del repo debe honrar ese env.

## Manifiesto (`atlas.yml`)

```yaml
build:
  minify: true          # default si se omite

exposure:
  default: public
  requireTls: true      # default si se omite; aplica a PUBLIC
```

Ver ejemplo: [atlas.project.example.yml](../schemas/atlas.project.example.yml).

## Comportamiento en deploy

1. Tras resolver compose desde `atlas.yml` / `composePath`.
2. Si minify → asegura línea `NODE_ENV=production` en `.env` (crea o actualiza solo esa key).
3. Si PUBLIC + `requireTls` → log de política TLS; metadata Traefik debe reportar `tls=true` (path Autopilot existente).

## Qué no hace Atlas (aún)

- Auditoría post-build del bundle (source maps, tamaño).
- HSTS obligatorio.
- Crypto de payload JSON.
- Fallar deploy si Tunnel ensure queda en modo MANUAL (sigue copy-assist).

## Relacionado

- Edge: [public-customer-hostname.md](../deployment/public-customer-hostname.md)
- Placement: [autopilot-placement.md](./autopilot-placement.md)
- Secrets at-rest: [config-security.md](../modules/config-security.md)
