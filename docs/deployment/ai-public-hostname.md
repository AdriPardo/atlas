# AI stack public hostname (`ai.atlasops.dev`)

Open WebUI + Ollama live on AI VM `192.168.1.26` (UFW: only platform `192.168.1.24`). Platform host runs nginx `ai-proxy` and Traefik.

## URLs

| Surface | URL | Auth |
|---------|-----|------|
| WebUI (public) | `https://ai.atlasops.dev` | Authentik ForwardAuth (Atlas Admins) → Open WebUI OIDC (same IdP, no password form) |
| WebUI (LAN) | `http://192.168.1.24:3001` | Open WebUI OIDC → Authentik (no ForwardAuth on LAN) |
| Ollama API | `http://192.168.1.24:11434` | LAN only — **not** published on Traefik/Tunnel |

## How login works (SSO)

1. User opens `https://ai.atlasops.dev`.
2. Traefik Authentik **ForwardAuth** (app **AI** / Proxy provider) — one Authentik login if no session.
3. Open WebUI has `ENABLE_LOGIN_FORM=false` + `OAUTH_AUTO_REDIRECT=true` → redirects to Authentik **OIDC** (app **Open WebUI OIDC**).
4. Existing Authentik session → silent consent → callback → chat. **No WebUI password form.**

Emails match Authentik ↔ WebUI (`OAUTH_MERGE_ACCOUNTS_BY_EMAIL=true`) so existing local users (e.g. admin) merge on first OIDC login.

## Platform pieces (`192.168.1.24`)

- Compose: `/opt/atlas-data/compose/ai-proxy/`
  - Traefik labels: `Host(\`ai.atlasops.dev\`)` + middleware `authentik,securityHeaders@file,gzip@file`
  - Upstream: `192.168.1.26:3000` (WebUI), `:11434` (Ollama)
- Authentik:
  - Application **AI** / Proxy provider (ForwardAuth on Embedded Outpost) — group **Atlas Admins**
  - Application **Open WebUI OIDC** / OAuth2 provider — redirect `https://ai.atlasops.dev/oauth/oidc/callback`, group **Atlas Admins**, slug `open-webui-oidc`
- DNS: CNAME `ai` → `<tunnel-id>.cfargotunnel.com` (proxied)

## AI VM Open WebUI (`192.168.1.26`)

- systemd user unit: `~/.config/systemd/user/open-webui.service`
- Env file (mode 600): `/home/atlas/apps/open-webui/open-webui.env`
- Key vars: `WEBUI_URL`, `ENABLE_OAUTH`, `ENABLE_OAUTH_SIGNUP`, `OAUTH_*`, `OPENID_*`, `OAUTH_AUTO_REDIRECT=true`, `ENABLE_LOGIN_FORM=false`, `ENABLE_PASSWORD_AUTH=false`, `ENABLE_SIGNUP=false`
- Persistent DB also set: `webui.url`, `ui.enable_login_form=false`, `ui.default_user_role=user`
- OIDC discovery: `https://auth.atlasops.dev/application/o/open-webui-oidc/.well-known/openid-configuration`

Restart after env change:

```bash
ssh atlas@192.168.1.26 'systemctl --user daemon-reload && systemctl --user restart open-webui'
```

## Manual: Tunnel Public Hostname

Tunnel is **remotely managed** (token). The Zone DNS token on the host cannot edit tunnel ingress (same pattern as Reelpath before Ensure had Tunnel Edit scope).

Zero Trust → Networks → Tunnels → atlas tunnel → **Public Hostname** → Add:

| Field | Value |
|-------|--------|
| Subdomain | `ai` |
| Domain | `atlasops.dev` |
| Type | HTTPS |
| URL | `traefik:443` |
| TLS | **No TLS Verify** = on |

Copy block also in `/home/atlas/cloudflared/README-ai-ingress.md` on the platform host.

Optional: extend `cloudflare.api.token` with Account → Cloudflare Tunnel / Cloudflare One → Edit so Atlas Autopilot / API can push ingress later.

## First admin / bootstrap

Admin already exists (local signup before SSO). First OIDC login with matching Authentik email merges into that admin.

If greenfield:

1. Temporarily set `ENABLE_LOGIN_FORM=true` / `ENABLE_PASSWORD_AUTH=true` **or** create admin via LAN before locking forms.
2. Or rely on first OAuth user becoming admin when DB has no users (`ENABLE_OAUTH_SIGNUP=true`).
3. Then disable password form again as above.

## Test

1. Incognito → `https://ai.atlasops.dev`
2. Authentik login (Atlas Admins).
3. Expect redirect hop to OIDC then Open WebUI chat — **no** WebUI email/password screen.
4. Confirm user matches Authentik email.

## Do not

- Do not expose Ollama on a public hostname without a separate auth story (ForwardAuth breaks CLI API clients).
- Do not open UFW on `.26` beyond `.24`.
- Do not attach Authentik to customer apps (Reelpath) — AI is a platform ops surface, like Grafana.
- Do not enable `WEBUI_AUTH_TRUSTED_*` headers while LAN `:3001` is reachable without ForwardAuth (header spoof risk). Prefer OIDC.
- Do not set Authentik OIDC **Encryption Key** (Open WebUI does not support encrypted tokens).
