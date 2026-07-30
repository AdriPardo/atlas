# Graphify + Obsidian en Atlas

Knowledge graph del monorepo ([Graphify](https://github.com/Graphify-Labs/graphify) / PyPI `graphifyy`) + vault Obsidian sobre `docs/`.

## Qué hay en el repo

| Pieza | Ruta | Rol |
|-------|------|-----|
| Regla Cursor (always-on) | `.cursor/rules/graphify.mdc` | Agente lee/consulta el grafo antes de Grep/Glob |
| Skill `/graphify` | `.cursor/skills/graphify/SKILL.md` | Pipeline oficial Graphify en Cursor |
| Ignore de extracción | `.graphifyignore` | Excluye build, node_modules, ruido |
| Salida del grafo | `graphify-out/` | `GRAPH_REPORT.md`, `graph.json`, `graph.html` |
| Vault Obsidian | `docs/` (`.obsidian/`) | Abrir esta carpeta como vault |
| Notas generadas Graphify | `docs/graphify/` | Export `--obsidian` (gitignored) |

## Requisitos locales (una vez)

Python **3.10+** (sistema actual con solo 3.9 → instalar 3.12). Paquete oficial: **`graphifyy`** (doble y). CLI: `graphify`.

```bash
# recomendado
curl -LsSf https://astral.sh/uv/install.sh | sh
uv tool install graphifyy

# o
pipx install graphifyy

# registrar integración Cursor en este repo (ya hay regla; re-ejecutar refresca)
graphify cursor install
```

Script opcional del repo:

```bash
./scripts/setup-graphify.sh
```

## Construir / actualizar el grafo

Desde la raíz del monorepo:

```bash
# primera vez (AST + semántica vía el asistente; en Cursor: /graphify .)
graphify .

# solo código cambiado (AST, sin coste LLM)
graphify update .

# grafo + notas Obsidian dentro del vault docs/
graphify . --obsidian --obsidian-dir docs/graphify
```

Salida típica en `graphify-out/`:

- `GRAPH_REPORT.md` — mapa de god nodes / comunidades
- `graph.json` — grafo queryable (`graphify query`, `path`, `explain`)
- `graph.html` — viz interactiva en el navegador

Consultas:

```bash
graphify query "cómo fluye el deploy compose"
graphify path "Project" "Deployment"
graphify explain "Authentik SSO"
```

## Abrir vault en Obsidian

1. Instalar [Obsidian](https://obsidian.md/).
2. **Open folder as vault** → elegir `…/atlas/docs` (esta carpeta, no la raíz del repo).
3. Graph view nativo: enlaces `[[wiki]]` entre ADRs / módulos.
4. Tras `graphify . --obsidian --obsidian-dir docs/graphify`, carpeta `graphify/` dentro del vault con notas + `graph.canvas`.

No hace falta plugin Community para el grafo básico; Graphify escribe markdown/canvas compatibles.

## Git

En `.gitignore`:

- `graphify-out/cache/`, `manifest.json`, `cost.json` — locales
- `docs/graphify/` — export Obsidian regenerable
- `docs/.obsidian/workspace*` — estado UI local

Conviene **commitear** `GRAPH_REPORT.md` + `graph.json` (+ opcional `graph.html`) cuando el grafo esté estable, para que el equipo arranque con mapa.

## Alternativas no usadas aquí

- Vault dedicado fuera de `docs/` (`~/vaults/atlas`) — posible con `--obsidian-dir`
- MCP `python -m graphify.serve graphify-out/graph.json` — opt-in; ver README upstream
- Neo4j / FalkorDB — flags `--neo4j` / `--falkordb` si hace falta grafo externo
