# Módulos — Networking

## Domains

Hostname asociado a Project (y opcionalmente Service). Estados: `PENDING_DNS` | `ACTIVE` | `ERROR`.

API (v0.7):

- `GET/POST /api/v1/projects/{projectId}/domains`
- `GET/PUT/DELETE /api/v1/domains/{id}`
- `POST /api/v1/domains/{id}/verify` — acepta ownership en control plane; emite TXT `_atlas-challenge.<host>`

## Certificates

Metadata embebida en Domain (`certificateIssuer`, `certificateExpiresAt`, `certificateSans`). Tras verify stub: issuer `letsencrypt-stub`, SAN = hostname, expiry +90d. Renovación real: job o Traefik ACME.

## DNS

Challenge TXT expuesto en la respuesta Domain (`dnsTxtName` / `dnsTxtValue`). Sync opcional vía `DnsProviderPort` (stub hoy; Cloudflare API después).

## Cloudflare

Provider adapter stub: documenta el TXT a crear manualmente. Token en Secrets + API sync = incremento futuro.

## Traefik

Desired route metadata generado por `TraefikMetadataPort` / `StaticTraefikMetadataAdapter`:

- `GET /api/v1/domains/{id}/traefik`
- alias `GET /api/v1/traefik/routes/{id}`

Labels típicas: `traefik.enable`, router `Host(...)`, TLS + certresolver, service port (`atlas.networking.traefik-*`).
Atlas es control plane; aplicar labels al data plane queda a ops/compose.

