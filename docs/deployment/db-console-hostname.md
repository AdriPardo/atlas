# Project DB console (`/db-console`)

Managed **pgweb** SQL UI for Atlas project schemas in the shared `apps` database.

## URL

| Surface | URL | Auth |
|---------|-----|------|
| Console | `https://atlas.atlasops.dev/db-console/` | Authentik ForwardAuth (Provider for Atlas — same SSO as Atlas UI) |
| From Atlas | Project → Database → **Open database** | Mints TTL role + opens pgweb via Connect Backend (already connected) |

Path under the existing Atlas host avoids a new Cloudflare Tunnel Public Hostname. Optional later: `db.atlasops.dev` (same pattern as [ai-public-hostname.md](ai-public-hostname.md)).

## How it works

1. User clicks **Open database** (profile + TTL from the panel; default `db.read` / 60m).
2. Atlas `POST /api/v1/projects/{id}/database/console-session` creates an ephemeral Postgres role and a **one-time ticket** (password never returned to the browser).
3. Browser opens `https://atlas.atlasops.dev/db-console/connect/{ticket}` (SSO cookie already on host).
4. pgweb calls Atlas `POST /api/v1/internal/pgweb/connect` with the shared connect token; Atlas returns `database_url` once.
5. pgweb redirects to `/db-console/?session=…` → SQL UI for that schema only (TTL role grants + `search_path`).

### Why `?url=` failed

pgweb in `--sessions` mode does **not** auto-connect from a `?url=` query string (that was never a supported deep-link). The SPA only auto-attaches an existing session via `?session=`. CLI `--url` is cleared when `--sessions` is on. Fix = official **Connect Backend** (`/connect/:resource`).

## Platform pieces (`192.168.1.24`)

- Compose: `/opt/atlas-data/compose/db-console/` (source: `deploy/db-console/` in repo)
- Traefik: `Host(\`atlas.atlasops.dev\`) && PathPrefix(\`/db-console\`)` priority 50 + middleware `authentik,securityHeaders@file,gzip@file` (pgweb `--prefix=db-console`; **no** StripPrefix)
- Networks: `atlas-public` + `atlas-internal` (pgweb → `backend:8080` + hostname `postgres`, DB `apps`)
- Atlas `.env`: `ATLAS_DB_CONSOLE_URL` + `ATLAS_DB_CONSOLE_CONNECT_TOKEN` (must match `PGWEB_CONNECT_TOKEN`)

## Security

- No public unauthenticated console.
- Default target is never control-plane DB `atlas`.
- Least privilege via TTL roles (prefer `db.read`): `USAGE`/`SELECT` on project schema only.
- Password not left in browser history / screenshotable query string.
- Limitation: pgweb sessions mode still allows typing another connection string if the operator knows credentials (same class of risk as LAN `psql`). Prefer the Open button. Tickets are in-memory (single backend replica).

## Upgrade path

CloudBeaver / Bytebase for richer multi-user audit and saved queries. Keep Authentik in front.
