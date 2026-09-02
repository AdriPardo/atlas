#!/usr/bin/env bash
# Register Mochi project in Atlas (idempotent: skips if name "mochi" exists).
#
# Usage:
#   export ATLAS_API_BASE=http://192.168.1.24:8080   # optional
#   export ATLAS_ADMIN_USERNAME=... ATLAS_ADMIN_PASSWORD=...
#   ./scripts/register-mochi-project.sh
#
set -euo pipefail

API_BASE="${ATLAS_API_BASE:-http://192.168.1.24:8080}"
REPO_URL="${MOCHI_REPO_URL:-https://github.com/AdriPardo/mochi.git}"
BRANCH="${MOCHI_BRANCH:-master}"
DOMAIN="${MOCHI_DOMAIN:-mochi.atlasops.dev}"

log() { printf '[register-mochi] %s\n' "$*"; }
die() { printf '[register-mochi] ERROR: %s\n' "$*" >&2; exit 1; }

command -v curl >/dev/null || die "curl required"
command -v python3 >/dev/null || die "python3 required"

login() {
  if [[ -n "${ATLAS_TOKEN:-}" ]]; then
    TOKEN="$ATLAS_TOKEN"
    return
  fi
  [[ -n "${ATLAS_ADMIN_USERNAME:-}" ]] || die "set ATLAS_ADMIN_USERNAME (+ password) or ATLAS_TOKEN"
  [[ -n "${ATLAS_ADMIN_PASSWORD:-}" ]] || die "set ATLAS_ADMIN_PASSWORD or ATLAS_TOKEN"
  TOKEN="$(python3 -c 'import json,os,urllib.request; body=json.dumps({"username":os.environ["ATLAS_ADMIN_USERNAME"],"password":os.environ["ATLAS_ADMIN_PASSWORD"]}).encode(); req=urllib.request.Request("'$API_BASE'/api/v1/auth/login", data=body, headers={"Content-Type":"application/json"}); print(json.load(urllib.request.urlopen(req)).get("accessToken") or json.load(urllib.request.urlopen(req)).get("token") or "")' 2>/dev/null || true)"
  if [[ -z "${TOKEN:-}" ]]; then
    LOGIN_BODY=$(python3 -c 'import json,os; print(json.dumps({"username":os.environ["ATLAS_ADMIN_USERNAME"],"password":os.environ["ATLAS_ADMIN_PASSWORD"]}))')
    TOKEN=$(curl -sS -X POST "${API_BASE}/api/v1/auth/login" -H 'Content-Type: application/json' -d "$LOGIN_BODY" \
      | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("accessToken") or d.get("token") or "")')
  fi
  [[ -n "$TOKEN" ]] || die "login failed"
  log "login OK"
}

find_existing() {
  curl -sS "${API_BASE}/api/v1/projects?size=100" -H "Authorization: Bearer ${TOKEN}" \
    | python3 -c '
import sys, json
for p in json.load(sys.stdin).get("content", []):
    if p.get("name", "").lower() == "mochi":
        print(p["id"])
        break
'
}

create_project() {
  local payload
  payload=$(python3 -c 'import json,os; print(json.dumps({
    "name": "mochi",
    "description": "Mochi virtual pet — API + admin panel",
    "repositoryUrl": os.environ["REPO_URL"],
    "branch": os.environ["BRANCH"],
    "domain": os.environ["DOMAIN"],
  }))')
  curl -sS -X POST "${API_BASE}/api/v1/projects" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "$payload"
}

main() {
  export REPO_URL BRANCH DOMAIN
  login
  EXISTING="$(find_existing || true)"
  if [[ -n "$EXISTING" ]]; then
    log "project already exists id=$EXISTING"
    echo "MOCHI_PROJECT_ID=$EXISTING"
    exit 0
  fi
  RESP="$(create_project)"
  PROJECT_ID=$(printf '%s' "$RESP" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("id",""))')
  if [[ -z "$PROJECT_ID" ]]; then
    echo "$RESP" >&2
    die "create project failed"
  fi
  log "created project id=$PROJECT_ID domain=$DOMAIN"
  echo "MOCHI_PROJECT_ID=$PROJECT_ID"
}

main "$@"
