#!/usr/bin/env bash
# Idempotently patch docker-compose.prod.yml with Traefik routers required for SSO + API JWT.
set -euo pipefail

APP_DIR="${ATLAS_APP_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
PROD="$APP_DIR/docker-compose.prod.yml"

log() { printf '[traefik] %s\n' "$*"; }

if [[ ! -f "$PROD" ]]; then
  log "no docker-compose.prod.yml — skip"
  exit 0
fi

if grep -q 'traefik.http.routers.atlas-api.rule' "$PROD"; then
  log "atlas-api router already present"
  exit 0
fi

log "patching $PROD"

python3 - "$PROD" <<'PY'
import pathlib
import sys

path = pathlib.Path(sys.argv[1])
text = path.read_text()

insert = """      - "traefik.http.routers.atlas-api.rule=Host(`atlas.atlasops.dev`) && PathPrefix(`/api/`)"
      - "traefik.http.routers.atlas-api.entrypoints=websecure"
      - "traefik.http.routers.atlas-api.tls=true"
      - "traefik.http.routers.atlas-api.tls.certresolver=cloudflare"
      - "traefik.http.routers.atlas-api.middlewares=securityHeaders@file,gzip@file"
      - "traefik.http.routers.atlas-api.service=atlas"
      - "traefik.http.routers.atlas-api.priority=90"
"""

anchors = [
    '      - "traefik.http.routers.atlas-webhooks.rule',
    '      - "traefik.http.routers.atlas.priority=1"',
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
