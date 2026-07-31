#!/usr/bin/env bash
# Optional bulk upsert of user/app secrets into Atlas from env / .env.secrets.
# Prefer UI (Project → Secrets / Org secrets) for day-to-day. Idempotent. Never prints values.
#
# Usage:
#   cp scripts/env.secrets.example .env.secrets   # fill values; file is gitignored
#   export ATLAS_ADMIN_USERNAME=... ATLAS_ADMIN_PASSWORD=...
#   ./scripts/seed-project-secrets.sh
#
# Scope:
#   ATLAS_SECRET_SCOPE=org|project   (default: org)
#   ATLAS_PROJECT_ID=<uuid>          (required when scope=project)
#   ATLAS_API_BASE=http://127.0.0.1:8080
#   ATLAS_SECRETS_FILE=.env.secrets  (optional; also reads process env)
#   ATLAS_TOKEN=<jwt>                (optional; skips login if set)
#
# Extra logical names (comma-separated). Each needs an env var with the same name
# uppercased, dots/hyphens → underscores (e.g. stripe.secret → STRIPE_SECRET):
#   ATLAS_EXTRA_SECRETS=stripe.secret,pexels.api_key
#
# Optional Reelpath bridge (migration): after Atlas upsert, also upsert PlatformSecret
# in the app DB via docker exec:
#   REELPATH_SEED_PLATFORM=1
#   REELPATH_API_CONTAINER=reelpath-api-1
#
set -euo pipefail

API_BASE="${ATLAS_API_BASE:-http://127.0.0.1:8080}"
SCOPE="${ATLAS_SECRET_SCOPE:-org}"
SECRETS_FILE="${ATLAS_SECRETS_FILE:-.env.secrets}"
PROJECT_ID="${ATLAS_PROJECT_ID:-}"

log() { printf '[seed-secrets] %s\n' "$*"; }
die() { printf '[seed-secrets] ERROR: %s\n' "$*" >&2; exit 1; }

command -v curl >/dev/null || die "curl required"
command -v python3 >/dev/null || die "python3 required (JSON encode)"

# Load .env.secrets into this shell without exporting unrelated noise if file missing.
if [[ -f "$SECRETS_FILE" ]]; then
  log "loading $SECRETS_FILE"
  set -a
  # shellcheck disable=SC1090
  source "$SECRETS_FILE"
  set +a
elif [[ -f ".env.secrets" && "$SECRETS_FILE" != ".env.secrets" ]]; then
  log "loading .env.secrets"
  set -a
  # shellcheck disable=SC1091
  source ".env.secrets"
  set +a
fi

# Env var → Atlas logical secret name (known conventions)
# Format: ENV_VAR|atlas.logical.name
MAPPINGS=(
  "OPENAI_API_KEY|ai.openai"
  "OPENAI_BASE_URL|ai.openai.base_url"
  "ELEVENLABS_API_KEY|ai.elevenlabs"
  "DEEPSEEK_API_KEY|ai.deepseek"
  "AI_PROVIDER|ai.provider"
  "AI_API_KEY|ai.api_key"
  "AI_BASE_URL|ai.base_url"
  "GIT_TOKEN|git.token"
  "CLOUDFLARE_API_TOKEN|cloudflare.api.token"
  "DB_URL|db.url"
  "STRIPE_SECRET_KEY|stripe.secret"
)

json_escape() {
  python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()), end="")' <<<"$1"
}

logical_to_env() {
  # stripe.secret → STRIPE_SECRET
  echo "$1" | tr '.-' '__' | tr '[:lower:]' '[:upper:]'
}

login() {
  if [[ -n "${ATLAS_TOKEN:-}" ]]; then
    TOKEN="$ATLAS_TOKEN"
    log "using ATLAS_TOKEN"
    return
  fi
  [[ -n "${ATLAS_ADMIN_USERNAME:-}" ]] || die "set ATLAS_ADMIN_USERNAME (+ password) or ATLAS_TOKEN"
  [[ -n "${ATLAS_ADMIN_PASSWORD:-}" ]] || die "set ATLAS_ADMIN_PASSWORD or ATLAS_TOKEN"

  local user_json pass_json body
  user_json="$(json_escape "$ATLAS_ADMIN_USERNAME")"
  pass_json="$(json_escape "$ATLAS_ADMIN_PASSWORD")"
  body="{\"username\":${user_json},\"password\":${pass_json}}"

  TOKEN="$(curl -sS -X POST "${API_BASE}/api/v1/auth/login" \
    -H 'Content-Type: application/json' \
    -d "$body" \
    | python3 -c 'import sys,json; print(json.load(sys.stdin).get("token") or "")')"
  [[ -n "$TOKEN" ]] || die "login failed (empty token). Check credentials / ATLAS_API_BASE"
  log "login OK"
}

upsert_one() {
  local name="$1" value="$2" url http code
  local name_json value_json payload
  name_json="$(json_escape "$name")"
  value_json="$(json_escape "$value")"
  payload="{\"name\":${name_json},\"value\":${value_json}}"

  if [[ "$SCOPE" == "org" ]]; then
    url="${API_BASE}/api/v1/secrets"
  elif [[ "$SCOPE" == "project" ]]; then
    [[ -n "$PROJECT_ID" ]] || die "ATLAS_PROJECT_ID required when ATLAS_SECRET_SCOPE=project"
    url="${API_BASE}/api/v1/projects/${PROJECT_ID}/secrets"
  else
    die "ATLAS_SECRET_SCOPE must be org or project (got: $SCOPE)"
  fi

  # Capture status; body may contain id/name — never echo value
  http="$(curl -sS -o /tmp/atlas-seed-secret-resp.json -w '%{http_code}' \
    -X PUT "$url" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'Content-Type: application/json' \
    -d "$payload")"
  code="$http"
  if [[ "$code" != "200" && "$code" != "201" ]]; then
    printf '[seed-secrets] ERROR: upsert %s → HTTP %s\n' "$name" "$code" >&2
    python3 -c 'import pathlib; p=pathlib.Path("/tmp/atlas-seed-secret-resp.json"); print(p.read_text()[:500] if p.exists() else "")' >&2 || true
    die "upsert failed for $name"
  fi
  if [[ "$code" == "201" ]]; then
    log "created $name (scope=$SCOPE)"
  else
    log "updated $name (scope=$SCOPE)"
  fi
}

seed_atlas() {
  local count=0
  for pair in "${MAPPINGS[@]}"; do
    local env_key="${pair%%|*}"
    local logical="${pair##*|}"
    local val="${!env_key:-}"
    if [[ -z "$val" ]]; then
      continue
    fi
    upsert_one "$logical" "$val"
    count=$((count + 1))
  done

  # Free-form extras: ATLAS_EXTRA_SECRETS=name1,name2 — value from SCREAMING_SNAKE env
  if [[ -n "${ATLAS_EXTRA_SECRETS:-}" ]]; then
    local IFS=','
    # shellcheck disable=SC2086
    set -- ${ATLAS_EXTRA_SECRETS}
    for logical in "$@"; do
      logical="$(echo "$logical" | xargs)"
      [[ -n "$logical" ]] || continue
      local env_key
      env_key="$(logical_to_env "$logical")"
      local val="${!env_key:-}"
      if [[ -z "$val" ]]; then
        log "skip extra $logical (env $env_key empty)"
        continue
      fi
      upsert_one "$logical" "$val"
      count=$((count + 1))
    done
  fi

  if [[ "$count" -eq 0 ]]; then
    die "no secret env vars set. Fill $SECRETS_FILE or export mapped vars / ATLAS_EXTRA_SECRETS."
  fi
  log "Atlas upserted $count secret(s) → scope=$SCOPE"
}

# Bridge: upsert Reelpath PlatformSecret from same env (migration only).
seed_reelpath_platform() {
  local container="${REELPATH_API_CONTAINER:-reelpath-api-1}"
  command -v docker >/dev/null || die "docker required for REELPATH_SEED_PLATFORM=1"
  docker inspect "$container" >/dev/null 2>&1 || die "container not found: $container"

  log "Reelpath PlatformSecret upsert via $container (names only logged)"

  docker exec \
    -e OPENAI_API_KEY="${OPENAI_API_KEY:-}" \
    -e DEEPSEEK_API_KEY="${DEEPSEEK_API_KEY:-}" \
    -e ELEVENLABS_API_KEY="${ELEVENLABS_API_KEY:-}" \
    -e PEXELS_API_KEY="${PEXELS_API_KEY:-}" \
    "$container" node --input-type=module -e '
const mod = await import("@autotube/database");
const upserted = [];
async function one(provider, value) {
  const v = (value || "").trim();
  if (!v) return;
  await mod.upsertPlatformApiKey(provider, v);
  upserted.push(provider);
}
await one("openai", process.env.OPENAI_API_KEY);
await one("deepseek", process.env.DEEPSEEK_API_KEY);
await one("elevenlabs", process.env.ELEVENLABS_API_KEY);
await one("pexels", process.env.PEXELS_API_KEY);
await mod.loadPlatformSecretsOverrides();
console.log(JSON.stringify({ upserted }));
await mod.prisma.$disconnect();
'
  log "Reelpath PlatformSecret done (migration bridge; prefer Atlas envFrom)"
}

main() {
  login
  seed_atlas
  if [[ "${REELPATH_SEED_PLATFORM:-0}" == "1" ]]; then
    seed_reelpath_platform
  fi
  log "done — redeploy project so envFrom materializes .env (or wait next deploy)"
}

main "$@"
