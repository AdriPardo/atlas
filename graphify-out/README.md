# graphify-out/

Salida de [Graphify](https://github.com/Graphify-Labs/graphify) para Atlas.

## Generar

```bash
# CLI (paquete PyPI: graphifyy)
uv tool install graphifyy   # una vez; Python 3.10+
graphify .

# o en Cursor: /graphify .
```

Archivos esperados tras el primer run:

- `GRAPH_REPORT.md` — resumen para el agente
- `graph.json` — grafo queryable
- `graph.html` — visualización

Ver guía: [docs/tooling/graphify-obsidian.md](../docs/tooling/graphify-obsidian.md).

Vault Obsidian del proyecto: abrir carpeta `docs/` en Obsidian. Export de nodos:

```bash
graphify . --obsidian --obsidian-dir docs/graphify
```
