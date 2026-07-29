# Public hostname for a customer app (Traefik + Cloudflare Tunnel)

Atlas deploys compose stacks onto the shared Docker host and relies on Traefik labels for routing.
Cloudflare Tunnel ingress is **remotely managed**. Autopilot assists registration; the DNS API token alone cannot add Public Hostnames.

## Already automated by Atlas

1. Service `domain` (e.g. `reelpath.atlasops.dev`) and Traefik `Host(...)` labels in the app compose.
2. Domain stub on PUBLIC deploy + Traefik metadata (`GET /api/v1/domains/{id}/traefik`).
3. **Tunnel ingress assist** (ADR-0011):
   - `GET /api/v1/domains/{id}/tunnel-ingress` — exact Zero Trust fields + copy block.
   - `POST /api/v1/domains/{id}/tunnel-ingress/ensure` — API register when configured; else `MANUAL`.
   - After PUBLIC `DEPLOY_SERVICE` success, deploy logs include mode + copy block if needed.
   - UI project Domains: **Tunnel** (copy) / **Ensure**.
4. **DNS CNAME** (ADR-0013):
   - `GET /api/v1/domains/{id}/dns-cname` — CNAME → `<tunnel-id>.cfargotunnel.com` (proxied).
   - `POST /api/v1/domains/{id}/dns-cname/ensure` — upsert when zone + token configured; else `MANUAL`.
   - Same PUBLIC deploy path also runs DNS ensure after Tunnel.
   - UI: **DNS** / **Ensure DNS**.

## Config for API automation (optional)

| Setting / secret | Purpose |
|------------------|---------|
| `ATLAS_CF_ACCOUNT_ID` | Cloudflare account id (Tunnel API) |
| `ATLAS_CF_TUNNEL_ID` | Remotely-managed tunnel id (+ CNAME target) |
| `ATLAS_CF_ZONE` | Zone name (subdomain split + DNS upsert) |
| `ATLAS_CF_ZONE_ID` | Optional zone id (skips zone name lookup) |
| `ATLAS_CF_TUNNEL_ORIGIN` | Default `https://traefik:443` |
| `ATLAS_CF_TUNNEL_NO_TLS_VERIFY` | Default `true` (match atlas edge) |
| Secret `cloudflare.api.token` | **Zone → DNS → Edit** + **Account → Cloudflare Tunnel / Cloudflare One → Edit** |

Without zone/tunnel/token, Atlas still returns the exact paste values (mode `MANUAL`).

### Token scopes

- Tunnel Public Hostname: Account → Cloudflare Tunnel / Cloudflare One → Edit.
- DNS CNAME: Zone → DNS → Edit (on `ATLAS_CF_ZONE`).
- One token with both scopes is fine; same secret name `cloudflare.api.token`.
- **HTTP 403** on Ensure → API/UI message `token scopes insufficient` (not a raw Cloudflare dump). Fix scopes under Org secrets / Project secrets, then retry. Copy blocks still work as fallback.

## Manual step (Zero Trust) — only if Ensure is MANUAL/FAILED

Cloudflare Zero Trust → Networks → Tunnels → (atlas tunnel) → **Public Hostname** → Add:

| Field | Value |
|-------|--------|
| Subdomain | from Tunnel preview (e.g. `reelpath`) |
| Domain | zone (e.g. `atlasops.dev`) |
| Type | HTTPS |
| URL | `traefik:443` |
| TLS | **No TLS Verify** = on |

Prefer copying from Atlas UI **Tunnel** / deploy logs instead of guessing.

DNS `CNAME` → `<tunnel-id>.cfargotunnel.com` (proxied): use **Ensure DNS** or the DNS copy block when API token lacks Zone DNS Edit.

Do **not** put Authentik ForwardAuth in front of customer apps unless requested.

## Compose pitfalls on `atlas-public`

Do not name compose services `postgres` / `redis` / `api` if the container also joins `atlas-public`:
Docker DNS will resolve those names to platform containers. Prefer `db` / `cache` / app-prefixed names.

## Reelpath auth bootstrap (Atlas)

Reelpath (`docker-compose.atlas.yml`) enables `AUTH_REQUIRED` but historically only ran migrations on API start — **no users** until seed.

- Seed creates owner from `DEFAULT_ADMIN_EMAIL` / `DEFAULT_ADMIN_PASSWORD` with `SEED_DEMO=false`.
- Compose should run `seed:ci` after migrate (idempotent upsert).
- **Migrate ownership:** API entrypoint already runs Prisma (`migrate:deploy:ci`). Until that moves out of the container start, **omit** `runtime.migrateCommand` in `atlas.yml` (Atlas hook would double-run). See [app-migrations.md](./app-migrations.md).
- Recovery if DB has orgs/plans but empty `User`:

```bash
docker exec -e DEFAULT_ADMIN_EMAIL=… -e DEFAULT_ADMIN_PASSWORD=… -e SEED_DEMO=false \
  reelpath-api-1 npm run seed:ci -w @autotube/database
```

Do not put long-lived admin passwords in chat logs; rotate after first login.
