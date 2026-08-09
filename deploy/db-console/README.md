# Atlas DB console (pgweb)

One-click SQL UI for project schemas in the shared `apps` database.

## URL

| Surface | URL | Auth |
|---------|-----|------|
| Console (public) | `https://atlas.atlasops.dev/db-console/` | Authentik ForwardAuth (Provider for Atlas) |
| From Atlas UI | Project → Database → **Open database** | Issues TTL role + one-time `/connect/{ticket}` |

## Security model

- Traefik middleware `authentik` — same SSO gate as Atlas UI (no anonymous pgweb).
- pgweb runs with `--prefix=db-console` (no StripPrefix) so Connect Backend redirects stay under `/db-console/`.
- Atlas mints short-lived Postgres roles (`db.read` default) scoped to the project schema; does not rotate `db.url`.
- **Open database** never puts the DB password in the browser URL. Flow:
  1. Atlas issues TTL role + stores connection string in a one-time ticket (2 min redeem window).
  2. Browser opens `/db-console/connect/{ticket}` (Authentik already satisfied).
  3. pgweb POSTs to Atlas `POST /api/v1/internal/pgweb/connect` with shared `--connect-token`.
  4. Atlas returns `{ "database_url": "…" }` once; pgweb creates a session and redirects to `/?session=…`.
- Console has **no** default connection to control-plane DB `atlas`.
- Operators with SSO can still type other connection strings inside pgweb if they know credentials (same class of risk as LAN `psql`). Prefer TTL Open button.

## Deploy

```bash
mkdir -p /opt/atlas-data/compose/db-console
cp docker-compose.yml /opt/atlas-data/compose/db-console/
TOKEN=$(openssl rand -hex 32)
# Same value on Atlas backend and pgweb:
grep -q ATLAS_DB_CONSOLE_CONNECT_TOKEN /opt/atlas/atlas/.env \
  || echo "ATLAS_DB_CONSOLE_CONNECT_TOKEN=$TOKEN" >> /opt/atlas/atlas/.env
printf 'PGWEB_CONNECT_TOKEN=%s\n' "$TOKEN" > /opt/atlas-data/compose/db-console/.env
cd /opt/atlas-data/compose/db-console && docker compose up -d
```

Atlas backend `.env`:

```bash
ATLAS_DB_CONSOLE_URL=https://atlas.atlasops.dev/db-console/
ATLAS_DB_CONSOLE_CONNECT_TOKEN=<same as PGWEB_CONNECT_TOKEN>
```

Redeploy Atlas backend so the Open button enables Connect Backend.

## Upgrade path

- CloudBeaver / Bytebase if multi-user audit + saved queries are needed.
- Optional hostname `db.atlasops.dev`: add Traefik `Host` rule + Cloudflare Tunnel Public Hostname (same pattern as `ai.atlasops.dev`).
