# Public hostname for a customer app (Traefik + Cloudflare Tunnel)

Atlas deploys compose stacks onto the shared Docker host and relies on Traefik labels for routing.
Cloudflare Tunnel ingress is **remotely managed**; the DNS API token alone cannot add Public Hostnames.

## Already automated by Atlas / DNS token

1. Service `domain` (e.g. `reelpath.atlasops.dev`) and Traefik `Host(...)` labels in the app compose.
2. DNS `CNAME` → `<tunnel-id>.cfargotunnel.com` (proxied), same target as `atlas.atlasops.dev`.

## Manual step (Zero Trust)

Cloudflare Zero Trust → Networks → Tunnels → (atlas tunnel) → **Public Hostname** → Add:

| Field | Value |
|-------|--------|
| Subdomain | `reelpath` (or app name) |
| Domain | `atlasops.dev` |
| Type | HTTPS |
| URL | `traefik:443` |
| TLS | **No TLS Verify** = on (same as `atlas.atlasops.dev`) |

Do **not** put Authentik ForwardAuth in front of customer apps unless requested.

## Compose pitfalls on `atlas-public`

Do not name compose services `postgres` / `redis` / `api` if the container also joins `atlas-public`:
Docker DNS will resolve those names to platform containers. Prefer `db` / `cache` / app-prefixed names.
