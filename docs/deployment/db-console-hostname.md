# Project DB console (`/db-console`)

Managed **pgweb** SQL UI for Atlas project schemas in the shared `apps` database.

## URL

| Surface | URL | Auth |
|---------|-----|------|
| Console | `https://atlas.atlasops.dev/db-console/` | Authentik ForwardAuth (Provider for Atlas — same SSO as Atlas UI) |
| From Atlas | Project → Database → **Open database** | Mints TTL role + opens pgweb pre-connected |

Path under the existing Atlas host avoids a new Cloudflare Tunnel Public Hostname. Optional later: `db.atlasops.dev` (same pattern as [ai-public-hostname.md](ai-public-hostname.md)).

## How it works

1. User clicks **Open database** (profile + TTL from the panel; default `db.read` / 60m).
2. Atlas `POST /api/v1/projects/{id}/database/console-session` creates an ephemeral Postgres role and returns a connection URL with `search_path` set to the project schema.
3. Browser opens pgweb with `?url=…` (SSO session already on `atlas.atlasops.dev`).
4. Browse tables / run SQL — grants limited by the TTL profile.

## Platform pieces (`192.168.1.24`)

- Compose: `/opt/atlas-data/compose/db-console/` (source: `deploy/db-console/` in repo)
- Traefik: `Host(\`atlas.atlasops.dev\`) && PathPrefix(\`/db-console\`)` priority 50 + middleware `authentik,securityHeaders@file,gzip@file`
- Networks: `atlas-public` + `atlas-internal` (reaches hostname `postgres`, DB `apps`)
- Atlas `.env`: `ATLAS_DB_CONSOLE_URL=https://atlas.atlasops.dev/db-console/`

## Security

- No public unauthenticated console.
- Default target is never control-plane DB `atlas`.
- Least privilege via TTL roles (prefer `db.read`).
- Limitation: pgweb sessions mode still allows typing another connection string if the operator knows credentials (same class of risk as LAN `psql`). Prefer the Open button.

## Upgrade path

CloudBeaver / Bytebase for richer multi-user audit and saved queries. Keep Authentik in front.
