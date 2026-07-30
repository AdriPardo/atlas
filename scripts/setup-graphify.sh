#!/usr/bin/env bash
# Install Graphify CLI (graphifyy) and refresh Cursor rule for this repo.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

need_python() {
  if command -v python3 >/dev/null 2>&1; then
    python3 - <<'PY'
import sys
raise SystemExit(0 if sys.version_info >= (3, 10) else 1)
PY
    return $?
  fi
  return 1
}

echo "==> Atlas Graphify setup"

if ! command -v graphify >/dev/null 2>&1; then
  if command -v uv >/dev/null 2>&1; then
    echo "Installing graphifyy via uv tool…"
    uv tool install graphifyy
  elif command -v pipx >/dev/null 2>&1; then
    echo "Installing graphifyy via pipx…"
    pipx install graphifyy
  else
    cat <<'EOF'
No se encontró `graphify`, `uv` ni `pipx`.

1) Instala Python 3.10+ (Homebrew: brew install python@3.12)
2) Instala uv:  curl -LsSf https://astral.sh/uv/install.sh | sh
3) Luego:       uv tool install graphifyy
4) Re-ejecuta:  ./scripts/setup-graphify.sh

Paquete PyPI oficial: graphifyy (doble y). CLI: graphify.
EOF
    exit 1
  fi
fi

if ! need_python; then
  echo "Aviso: Python del sistema < 3.10. Graphify necesita 3.10+."
  echo "Si `graphify` ya funciona (uv tool), puedes ignorar este aviso."
fi

echo "Refreshing Cursor rule…"
graphify cursor install

echo
echo "Listo. Siguiente:"
echo "  graphify .                                          # construir grafo"
echo "  graphify . --obsidian --obsidian-dir docs/graphify  # + vault Obsidian"
echo "  Abrir Obsidian → Open folder as vault → $ROOT/docs"
echo
echo "Guía: docs/tooling/graphify-obsidian.md"
