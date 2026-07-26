# Módulos — Networking

## Domains

Hostname asociado a Service o a ruta Traefik. Estados: `PENDING_DNS` | `ACTIVE` | `ERROR`.

## Certificates

Metadata de cert (issuer, expires_at, san). Renovación: job o defer a Traefik ACME; Atlas muestra expiración y alerta.

## DNS

Records deseados (CNAME/A/TXT). Sync opcional vía provider.

## Cloudflare

Provider adapter: API token en Secrets; gestiona DNS records y opcionalmente Tunnel routes documentación. No obliga a Cloudflare; es el provider prioritario del stack actual.

## Traefik

Modelo de **desired routes**: router rule, service upstream, middlewares (auth, rate-limit, headers).

Aplicación: generar labels dinámicas / file provider fragment montado / API Traefik según capacidad del entorno. Fallo de sync → estado `DRIFT` en UI.
