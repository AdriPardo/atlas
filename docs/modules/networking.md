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

Challenge TXT expuesto en la respuesta Domain (`dnsTxtName` / `dnsTxtValue`). Sync challenge sigue instructional.

CNAME Autopilot (ADR-0013) vía `DnsProviderPort` / `CloudflareDnsAdapter`:

- `GET /api/v1/domains/{id}/dns-cname` — copy-ready CNAME → tunnel target.
- `POST /api/v1/domains/{id}/dns-cname/ensure` — upsert cuando `ATLAS_CF_ZONE` (+ opcional `ZONE_ID`) + tunnel id + secret `cloudflare.api.token`; else `MANUAL`.
- Tras PUBLIC deploy: ensure automático (no falla el job). Ensure exitoso puede marcar Domain `ACTIVE`.

## Cloudflare

`CloudflareTunnelPort` / `CloudflareTunnelAdapter` (ADR-0011):

- `GET /api/v1/domains/{id}/tunnel-ingress` — copy-ready Public Hostname fields.
- `POST /api/v1/domains/{id}/tunnel-ingress/ensure` — API merge into remotely-managed tunnel when `ATLAS_CF_*` + secret `cloudflare.api.token` exist; else `MANUAL`.

Token scopes: Zone DNS Edit (CNAME) + Tunnel/Cloudflare One Edit (ingress). Un solo secret `cloudflare.api.token` puede cubrir ambos.

Si Ensure recibe **HTTP 403** de Cloudflare, `mode=FAILED` y el mensaje incluye `token scopes insufficient` (+ hint Org/Project secrets). La UI Domains enlaza a `/secrets`.

## Proxmox (Autopilot ISOLATED)

`VmProvisionerPort` / `ProxmoxVmProvisionerAdapter` (ADR-0012 guest-ready + REUSED):

- Deploy body `placementMode: SHARED | ISOLATED` (default SHARED).
- Config: `ATLAS_PROXMOX_API_URL`, `NODE`, `TEMPLATE_VMID`, …; secrets `proxmox.api.token` + `proxmox.ssh.private_key`.
- **Reuse:** Host SSH existente por hostname `atlas-…`, o VM Proxmox por nombre/tag = hostname (`ATLAS_PROXMOX_REUSE_ENABLED=true`) → `REUSED` sin clone.
- Sin match / sin credenciales / clone off / sin IP / sin SSH key → `STUBBED` (o fallback) y placement cae a shared LOCAL.
- Clone real: sin match + `ATLAS_PROXMOX_CLONE_ENABLED=true` → start + poll guest-agent (`ATLAS_PROXMOX_DEFAULT_GUEST_IP` opcional); tags de clone incluyen el hostname.
- Tras ready: Host SSH registrado + `SYNC_HOST`; `DEPLOY_SERVICE` en esa VM.

## Traefik

Desired route metadata generado por `TraefikMetadataPort` / `StaticTraefikMetadataAdapter`:

- `GET /api/v1/domains/{id}/traefik`
- alias `GET /api/v1/traefik/routes/{id}`

Labels típicas: `traefik.enable`, router `Host(...)`, TLS + certresolver, service port (`atlas.networking.traefik-*`).
Atlas es control plane; aplicar labels al data plane queda a ops/compose.

