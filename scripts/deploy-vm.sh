#!/usr/bin/env bash
# Alias for scripts/deploy.sh (production VM deploy).
# Prefer calling this from docs/CI that expect "deploy-vm.sh".
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec bash "$DIR/deploy.sh" "$@"
