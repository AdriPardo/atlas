#!/usr/bin/env bash
# Collect read-only inventory evidence from the Atlas host.
# Does not restart services. Skips secret file contents by default.
set -euo pipefail

MODE="local"
OUT_ROOT="inventory/raw/host"
SSH_HOST="${ATLAS_SSH_HOST:-}"
SSH_USER="${ATLAS_SSH_USER:-}"
SSH_KEY="${ATLAS_SSH_KEY:-}"
SEARCH_ROOTS="${ATLAS_SEARCH_ROOTS:-/opt /srv /home /etc /var/lib}"

usage() {
  cat <<'EOF'
Usage:
  collect-host-inventory.sh --local [--out DIR]
  collect-host-inventory.sh --remote [--out DIR]

Environment (remote):
  ATLAS_SSH_HOST      required for --remote
  ATLAS_SSH_USER      required for --remote
  ATLAS_SSH_KEY       optional private key path
  ATLAS_SEARCH_ROOTS  optional search roots for compose files
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --local) MODE="local"; shift ;;
    --remote) MODE="remote"; shift ;;
    --out) OUT_ROOT="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown arg: $1" >&2; usage; exit 1 ;;
  esac
done

TS="$(date -u +%Y%m%dT%H%M%SZ)"
OUT="${OUT_ROOT%/}/${TS}"
mkdir -p "$OUT"

ssh_base() {
  local opts=(-o BatchMode=yes -o StrictHostKeyChecking=accept-new)
  if [[ -n "$SSH_KEY" ]]; then
    opts+=(-i "$SSH_KEY")
  fi
  printf '%s\0' "${opts[@]}"
}

write_collector() {
  local target="$1"
  cat >"$target" <<'EOS'
#!/usr/bin/env bash
set -euo pipefail
OUTDIR="${1:?outdir required}"
SEARCH_ROOTS="${ATLAS_SEARCH_ROOTS:-/opt /srv /home /etc /var/lib}"
mkdir -p "$OUTDIR/compose-tree"

uname -a >"$OUTDIR/uname.txt" || true
[[ -f /etc/os-release ]] && cp /etc/os-release "$OUTDIR/os-release.txt" || true
df -h >"$OUTDIR/disk.txt" || true
free -h >"$OUTDIR/memory.txt" || true
ip -br a >"$OUTDIR/ip-addr.txt" 2>/dev/null || true
ss -tulpn >"$OUTDIR/listening-ports.txt" 2>/dev/null || true

if command -v docker >/dev/null 2>&1; then
  docker ps -a >"$OUTDIR/docker-ps.txt" || true
  docker network ls >"$OUTDIR/docker-networks.txt" || true
  docker volume ls >"$OUTDIR/docker-volumes.txt" || true
  docker images >"$OUTDIR/docker-images.txt" || true
  ids="$(docker ps -aq || true)"
  if [[ -n "${ids}" ]]; then
    # shellcheck disable=SC2086
    docker inspect ${ids} >"$OUTDIR/docker-inspect.json" || true
  fi
  : >"$OUTDIR/docker-networks-inspect.json"
  docker network ls -q | while read -r n; do
    docker network inspect "$n" >>"$OUTDIR/docker-networks-inspect.json" || true
  done
else
  echo "docker: not found" >"$OUTDIR/docker-ps.txt"
fi

{
  echo "=== systemctl list-units (filtered) ==="
  systemctl list-units --all --no-pager 2>/dev/null | grep -iE 'docker|traefik|cloudflare|prometheus|grafana|loki|alloy|cadvisor|node_exporter' || true
  echo "=== /etc/systemd/system (filtered) ==="
  ls -la /etc/systemd/system 2>/dev/null | grep -iE 'docker|traefik|cloudflare|prometheus|grafana|loki|alloy' || true
} >"$OUTDIR/systemd-units.txt"

{
  echo "=== user crontab ==="
  crontab -l 2>/dev/null || true
  echo "=== /etc/cron* ==="
  ls -la /etc/cron* 2>/dev/null || true
  for f in /etc/crontab /etc/cron.d/*; do
    [[ -f "$f" ]] || continue
    echo "----- $f -----"
    cat "$f" 2>/dev/null || true
  done
} >"$OUTDIR/cron.txt"

# shellcheck disable=SC2086
find $SEARCH_ROOTS \( -name 'docker-compose*.yml' -o -name 'docker-compose*.yaml' -o -name 'compose.yml' -o -name 'compose.yaml' \) 2>/dev/null | sort >"$OUTDIR/compose-files.txt" || true

while IFS= read -r compose; do
  [[ -z "$compose" ]] && continue
  dir="$(dirname "$compose")"
  rel="$(echo "$dir" | sed 's#^/##; s#/#__#g')"
  dest="$OUTDIR/compose-tree/$rel"
  mkdir -p "$dest"
  printf '%s\n' "$compose" >"$dest/COMPOSE_PATH.txt"
  find "$dir" -maxdepth 3 -type f \( \
      -name '*.yml' -o -name '*.yaml' -o -name '*.toml' -o -name '*.json' \
      -o -name '*.conf' -o -name '*.rules' -o -name '*.tmpl' -o -name '*.md' \
    \) ! -name '.env' ! -name '*.env' ! -iname '*secret*' ! -iname '*credential*' \
    ! -iname '*token*' ! -name '*.pem' ! -name '*.key' \
    -print0 2>/dev/null | while IFS= read -r -d '' f; do
      base="$(basename "$f")"
      hash="$(printf '%s' "$f" | sha1sum | awk '{print $1}' | cut -c1-8)"
      cp -a "$f" "$dest/${base}.${hash}" 2>/dev/null || true
    done
  if [[ -f "$dir/.env" ]]; then
    grep -E '^[A-Za-z_][A-Za-z0-9_]*=' "$dir/.env" 2>/dev/null | cut -d= -f1 | sort -u >"$dest/env-var-names.txt" || true
  fi
done <"$OUTDIR/compose-files.txt"

{
  echo "# Collection notes"
  echo "timestamp_utc: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "hostname: $(hostname)"
  echo "user: $(whoami)"
  echo "search_roots: $SEARCH_ROOTS"
} >"$OUTDIR/NOTES.md"

echo "Collection complete: $OUTDIR"
EOS
  chmod +x "$target"
}

COLLECTOR="$(mktemp)"
write_collector "$COLLECTOR"

if [[ "$MODE" == "local" ]]; then
  ATLAS_SEARCH_ROOTS="$SEARCH_ROOTS" bash "$COLLECTOR" "$OUT"
else
  if [[ -z "$SSH_HOST" || -z "$SSH_USER" ]]; then
    echo "ATLAS_SSH_HOST and ATLAS_SSH_USER are required for --remote" >&2
    rm -f "$COLLECTOR"
    exit 1
  fi
  mapfile -d '' -t SSH_OPTS < <(ssh_base)
  REMOTE_OUT="/tmp/atlas-inventory-${TS}"
  echo "Testing SSH ${SSH_USER}@${SSH_HOST}..."
  ssh "${SSH_OPTS[@]}" "${SSH_USER}@${SSH_HOST}" "echo ok" >/dev/null
  echo "Uploading collector..."
  scp "${SSH_OPTS[@]}" "$COLLECTOR" "${SSH_USER}@${SSH_HOST}:/tmp/atlas-collect.sh"
  ssh "${SSH_OPTS[@]}" "${SSH_USER}@${SSH_HOST}" \
    "ATLAS_SEARCH_ROOTS='${SEARCH_ROOTS}' bash /tmp/atlas-collect.sh '${REMOTE_OUT}'"
  scp -r "${SSH_OPTS[@]}" "${SSH_USER}@${SSH_HOST}:${REMOTE_OUT}/." "$OUT/"
  echo "Remote collection copied to $OUT"
fi

rm -f "$COLLECTOR"
echo "Done: $OUT"
