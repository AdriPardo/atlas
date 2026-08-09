# Atlas DB console (pgweb)

One-click SQL UI for project schemas in the shared `apps` database.

## URL

| Surface | URL | Auth |
|---------|-----|------|
| Console (public) | `https://atlas.atlasops.dev/db-console/` | Authentik ForwardAuth (Provider for Atlas) |
| From Atlas UI | Project → Database → **Open database** | Issues TTL role + opens console pre-connected |

## Security model

- Traefik middleware `authentik` — same SSO gate as Atlas UI (no anonymous Adminer/pgweb).
- Atlas mints short-lived Postgres roles (`db.read` default) scoped to the project schema; does not rotate `db.url`.
- Console has **no** default connection to control-plane DB `atlas`.
- Operators with SSO can still type other connection strings inside pgweb if they know credentials (same class of risk as LAN `psql`). Prefer TTL Open button.

## Deploy

```bash
mkdir -p /opt/atlas-data/compose/db-console
cp docker-compose.yml /opt/atlas-data/compose/db-console/
cd /opt/atlas-data/compose/db-console && docker compose up -d
```

Atlas backend `.env`:

```bash
ATLAS_DB_CONSOLE_URL=https://atlas.atlasops.dev/db-console/
```

Redeploy Atlas (or recreate backend) so the Open button enables.

## Upgrade path

- CloudBeaver / Bytebase if multi-user audit + saved queries are needed.
- Optional hostname `db.atlasops.dev`: add Traefik `Host` rule + Cloudflare Tunnel Public Hostname (same pattern as `ai.atlasops.dev`).
