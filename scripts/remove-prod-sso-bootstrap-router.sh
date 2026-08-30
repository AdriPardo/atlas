#!/usr/bin/env bash
# Remove deprecated atlas-sso-bootstrap Traefik router from prod compose.
# Bootstrap moved to /auth/sso/bootstrap (atlas router) — /api/ path caused Authentik 403.
set -euo pipefail

APP_DIR="${ATLAS_APP_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
PROD="$APP_DIR/docker-compose.prod.yml"

log() { printf '[traefik] %s\n' "$*"; }

if [[ ! -f "$PROD" ]]; then
  log "no docker-compose.prod.yml — skip"
  exit 0
fi

if ! grep -q 'traefik.http.routers.atlas-sso-bootstrap' "$PROD"; then
  log "atlas-sso-bootstrap router absent — OK"
  exit 0
fi

log "removing deprecated atlas-sso-bootstrap router from $PROD"

python3 - "$PROD" <<'PY'
import pathlib
import sys

path = pathlib.Path(sys.argv[1])
lines = path.read_text().splitlines()
out = []
skip_prefixes = (
    '      - "traefik.http.routers.atlas-sso-bootstrap.',
    "      - 'traefik.http.routers.atlas-sso-bootstrap.",
)
for line in lines:
    if any(line.startswith(p) for p in skip_prefixes):
        continue
    out.append(line)
path.write_text("\n".join(out) + ("\n" if lines and lines[-1].endswith("\n") else ""))
print("removed atlas-sso-bootstrap labels")
PY

log "cleanup OK"
