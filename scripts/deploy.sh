#!/usr/bin/env bash
# Idempotent production deploy for Atlas (dogfooding).
# Invoked by GitHub Actions over SSH, or manually on the VM.
set -euo pipefail

APP_DIR="${ATLAS_APP_DIR:-/opt/atlas/atlas}"
BRANCH="${ATLAS_DEPLOY_BRANCH:-master}"
REMOTE="${ATLAS_DEPLOY_REMOTE:-origin}"
HEALTH_RETRIES="${ATLAS_HEALTH_RETRIES:-36}"
HEALTH_SLEEP_SECS="${ATLAS_HEALTH_SLEEP_SECS:-5}"

log() { printf '[deploy] %s\n' "$*"; }
die() { printf '[deploy] ERROR: %s\n' "$*" >&2; exit 1; }

cd "$APP_DIR" || die "cannot cd to $APP_DIR"

# Load health URL overrides from .env (compose env_file does not export to this shell).
# Explicit environment still wins.
if [[ -f .env ]]; then
  for key in ATLAS_HEALTH_BACKEND_URL ATLAS_HEALTH_FRONTEND_URL ATLAS_HEALTH_RETRIES ATLAS_HEALTH_SLEEP_SECS; do
    if [[ -z "${!key:-}" ]]; then
      line="$(grep -E "^${key}=" .env | tail -n1 || true)"
      if [[ -n "$line" ]]; then
        export "$line"
      fi
    fi
  done
fi
HEALTH_BACKEND_URL="${ATLAS_HEALTH_BACKEND_URL:-http://127.0.0.1:8080/actuator/health}"
HEALTH_FRONTEND_URL="${ATLAS_HEALTH_FRONTEND_URL:-http://127.0.0.1:3000/}"
HEALTH_RETRIES="${ATLAS_HEALTH_RETRIES:-36}"
HEALTH_SLEEP_SECS="${ATLAS_HEALTH_SLEEP_SECS:-5}"

command -v git >/dev/null || die "git not found"
command -v docker >/dev/null || die "docker not found"
docker compose version >/dev/null 2>&1 || die "docker compose not available"

# Preserve production-only files (never overwrite)
for f in .env docker-compose.prod.yml docker-compose.override.yml; do
  if [[ -e "$f" ]]; then
    log "preserving local file: $f"
  fi
done

if [[ ! -d .git ]]; then
  die "$APP_DIR is not a git checkout"
fi

# Capture HEAD for rollback messaging
PREV_SHA="$(git rev-parse HEAD 2>/dev/null || echo unknown)"
log "previous HEAD: $PREV_SHA"

log "fetching $REMOTE..."
git fetch --prune "$REMOTE"

TARGET_REF="$REMOTE/$BRANCH"
git rev-parse --verify "$TARGET_REF" >/dev/null 2>&1 || die "missing ref $TARGET_REF"

# reset tracked files only — untracked (.env, *.prod.yml, override) stay intact
log "resetting hard to $TARGET_REF..."
git reset --hard "$TARGET_REF"

NEW_SHA="$(git rev-parse HEAD)"
log "deploying commit: $NEW_SHA"

if [[ -x scripts/ensure-prod-traefik-routers.sh ]]; then
  log "ensuring Traefik API routers..."
  bash scripts/ensure-prod-traefik-routers.sh
else
  log "WARN: scripts/ensure-prod-traefik-routers.sh missing or not executable"
fi

# Prefer production compose file if present (prod customizations survive reset --hard)
compose_args=()
if [[ -f docker-compose.prod.yml ]]; then
  log "using docker-compose.prod.yml"
  compose_args=(-f docker-compose.prod.yml)
elif [[ -f docker-compose.override.yml ]]; then
  log "using docker-compose.yml + docker-compose.override.yml"
  compose_args=(-f docker-compose.yml -f docker-compose.override.yml)
else
  log "using docker-compose.yml"
  compose_args=(-f docker-compose.yml)
fi

log "docker compose up -d --build..."
# Never use down -v — preserves volumes / data
set +e
docker compose "${compose_args[@]}" up -d --build
compose_rc=$?
set -e
if [[ "$compose_rc" -ne 0 ]]; then
  log "compose up exited $compose_rc — dumping recent backend logs"
  docker compose "${compose_args[@]}" logs backend --tail 120 || true
  docker compose "${compose_args[@]}" ps || true
  log "continuing to health polls (backend may still be starting)"
fi

wait_http() {
  local url="$1" label="$2" expect_body="${3:-}"
  local i code body
  for ((i = 1; i <= HEALTH_RETRIES; i++)); do
    code="$(curl -sS -o /tmp/atlas-health-body -w '%{http_code}' --max-time 5 "$url" || true)"
    body="$(cat /tmp/atlas-health-body 2>/dev/null || true)"
    if [[ "$code" =~ ^2 ]]; then
      if [[ -z "$expect_body" ]] || grep -q "$expect_body" <<<"$body"; then
        log "$label OK (HTTP $code) attempt $i/$HEALTH_RETRIES"
        return 0
      fi
    fi
    log "$label waiting... HTTP ${code:-n/a} ($i/$HEALTH_RETRIES)"
    sleep "$HEALTH_SLEEP_SECS"
  done
  die "$label failed after $HEALTH_RETRIES attempts ($url). Rollback: cd $APP_DIR && git reset --hard $PREV_SHA && docker compose ${compose_args[*]} up -d --build"
}

wait_http "$HEALTH_BACKEND_URL" "backend health" "UP"
wait_http "$HEALTH_FRONTEND_URL" "frontend"

log "deploy succeeded: $NEW_SHA"
