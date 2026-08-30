#!/usr/bin/env bash
# Ensure atlas-sso Traefik router exists (ForwardAuth on GET /api/v1/auth/sso).
set -euo pipefail

APP_DIR="${ATLAS_APP_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
PROD="$APP_DIR/docker-compose.prod.yml"

log() { printf '[traefik] %s\n' "$*"; }

if [[ ! -f "$PROD" ]]; then
  log "no docker-compose.prod.yml — skip"
  exit 0
fi

if grep -q 'traefik.http.routers.atlas-sso.rule' "$PROD"; then
  log "atlas-sso router already present"
  exit 0
fi

log "patching $PROD with atlas-sso router"

python3 - "$PROD" <<'PY'
import pathlib
import sys

path = pathlib.Path(sys.argv[1])
text = path.read_text()

insert = """      - "traefik.http.routers.atlas-sso.rule=Host(`atlas.atlasops.dev`) && PathPrefix(`/api/v1/auth/sso`)"
      - "traefik.http.routers.atlas-sso.entrypoints=websecure"
      - "traefik.http.routers.atlas-sso.tls=true"
      - "traefik.http.routers.atlas-sso.tls.certresolver=cloudflare"
      - "traefik.http.routers.atlas-sso.middlewares=authentik,securityHeaders@file,gzip@file"
      - "traefik.http.routers.atlas-sso.service=atlas"
      - "traefik.http.routers.atlas-sso.priority=95"
"""

anchors = [
    '      - "traefik.http.routers.atlas-api.rule',
    '      - "traefik.http.routers.atlas-webhooks.rule',
    '      - "traefik.http.services.atlas.loadbalancer.server.port=80"',
]

for anchor in anchors:
    if anchor in text:
        text = text.replace(anchor, insert + anchor, 1)
        path.write_text(text)
        print("patched via anchor:", anchor[:48])
        sys.exit(0)

print("ERROR: no anchor found in docker-compose.prod.yml", file=sys.stderr)
sys.exit(1)
PY

log "patch OK"
