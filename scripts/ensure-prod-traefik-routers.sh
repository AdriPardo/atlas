#!/usr/bin/env bash
# Idempotently patch docker-compose.prod.yml with Traefik routers required for SSO + API JWT.
# Safe to run on every deploy — skips when labels already present.
set -euo pipefail

APP_DIR="${ATLAS_APP_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
PROD="$APP_DIR/docker-compose.prod.yml"

if [[ ! -f "$PROD" ]]; then
  echo "[traefik] no docker-compose.prod.yml — skip"
  exit 0
fi

if grep -q 'traefik.http.routers.atlas-api' "$PROD"; then
  echo "[traefik] atlas-api router already present"
  exit 0
fi

echo "[traefik] patching $PROD with atlas-api + atlas-sso-bootstrap routers"

python3 - "$PROD" <<'PY'
import pathlib
import sys

path = pathlib.Path(sys.argv[1])
text = path.read_text()
marker = 'traefik.http.routers.atlas-webhooks.rule'
if marker not in text:
    print("[traefik] ERROR: cannot find atlas-webhooks block to anchor patch", file=sys.stderr)
    sys.exit(1)

insert = """      - "traefik.http.routers.atlas-sso-bootstrap.rule=Host(`atlas.atlasops.dev`) && PathPrefix(`/api/v1/auth/sso/bootstrap`)"
      - "traefik.http.routers.atlas-sso-bootstrap.entrypoints=websecure"
      - "traefik.http.routers.atlas-sso-bootstrap.tls=true"
      - "traefik.http.routers.atlas-sso-bootstrap.tls.certresolver=cloudflare"
      - "traefik.http.routers.atlas-sso-bootstrap.middlewares=authentik,securityHeaders@file,gzip@file"
      - "traefik.http.routers.atlas-sso-bootstrap.service=atlas"
      - "traefik.http.routers.atlas-sso-bootstrap.priority=110"
      - "traefik.http.routers.atlas-api.rule=Host(`atlas.atlasops.dev`) && PathPrefix(`/api/`)"
      - "traefik.http.routers.atlas-api.entrypoints=websecure"
      - "traefik.http.routers.atlas-api.tls=true"
      - "traefik.http.routers.atlas-api.tls.certresolver=cloudflare"
      - "traefik.http.routers.atlas-api.middlewares=securityHeaders@file,gzip@file"
      - "traefik.http.routers.atlas-api.service=atlas"
      - "traefik.http.routers.atlas-api.priority=90"
"""

anchor = '      - "traefik.http.routers.atlas-webhooks.rule'
if insert.strip().splitlines()[0] in text:
    print("[traefik] patch already applied")
    sys.exit(0)

text = text.replace(anchor, insert + anchor, 1)
path.write_text(text)
print("[traefik] patched successfully")
PY
