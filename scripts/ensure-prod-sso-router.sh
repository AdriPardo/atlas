#!/usr/bin/env bash
# Ensure atlas-auth Traefik router (ForwardAuth on /api/v1/auth/** for SSO mint fetch).
set -euo pipefail

APP_DIR="${ATLAS_APP_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
PROD="$APP_DIR/docker-compose.prod.yml"

log() { printf '[traefik] %s\n' "$*"; }

if [[ ! -f "$PROD" ]]; then
  log "no docker-compose.prod.yml — skip"
  exit 0
fi

python3 - "$PROD" <<'PY'
import pathlib
import re
import sys

path = pathlib.Path(sys.argv[1])
text = path.read_text()

AUTH_RULE = (
    '      - "traefik.http.routers.atlas-auth.rule=Host(`atlas.atlasops.dev`) && PathPrefix(`/api/v1/auth`)"\n'
    '      - "traefik.http.routers.atlas-auth.entrypoints=websecure"\n'
    '      - "traefik.http.routers.atlas-auth.tls=true"\n'
    '      - "traefik.http.routers.atlas-auth.tls.certresolver=cloudflare"\n'
    '      - "traefik.http.routers.atlas-auth.middlewares=authentik,securityHeaders@file,gzip@file"\n'
    '      - "traefik.http.routers.atlas-auth.service=atlas"\n'
    '      - "traefik.http.routers.atlas-auth.priority=95"\n'
)

if 'traefik.http.routers.atlas-auth.rule' in text:
    print('atlas-auth router already present')
    sys.exit(0)

# Migrate legacy atlas-sso (narrow /sso path) → atlas-auth
if 'traefik.http.routers.atlas-sso.rule' in text:
    text = re.sub(
        r'      - "traefik\.http\.routers\.atlas-sso\.[^"]+"\n',
        '',
        text,
    )
    anchors = [
        '      - "traefik.http.routers.atlas-api.rule',
        '      - "traefik.http.routers.atlas-webhooks.rule',
        '      - "traefik.http.services.atlas.loadbalancer.server.port=80"',
    ]
    for anchor in anchors:
        if anchor in text:
            text = text.replace(anchor, AUTH_RULE + anchor, 1)
            path.write_text(text)
            print('migrated atlas-sso → atlas-auth')
            sys.exit(0)

anchors = [
    '      - "traefik.http.routers.atlas-api.rule',
    '      - "traefik.http.routers.atlas-webhooks.rule',
    '      - "traefik.http.services.atlas.loadbalancer.server.port=80"',
]

for anchor in anchors:
    if anchor in text:
        text = text.replace(anchor, AUTH_RULE + anchor, 1)
        path.write_text(text)
        print('inserted atlas-auth router')
        sys.exit(0)

print('ERROR: no anchor found in docker-compose.prod.yml', file=sys.stderr)
sys.exit(1)
PY

log "patch OK"
