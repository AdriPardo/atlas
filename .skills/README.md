# Atlas agent skills

Skills de diseño traídas desde Autotube (sin el pack Flutter).

## Ubicaciones

| Ruta | Contenido |
|------|-----------|
| `.cursor/skills/` | Skills de Cursor (taste, impeccable, stitch, brandkit, etc.) |
| `.skills/` | Espejo de design skills + `emil-design-eng` |

## Sync automático

El hook `beforeSubmitPrompt` ejecuta `.cursor/hooks/sync-skills.sh` y actualiza:

- [emilkowalski/skill](https://github.com/emilkowalski/skill) → `.skills/emil-design-eng/`
- [Leonxlnx/taste-skill](https://github.com/Leonxlnx/taste-skill) → `.cursor/skills/*`
- [pbakaus/impeccable](https://github.com/pbakaus/impeccable) → `.cursor/skills/impeccable/`

No se sincronizan `flutter-ai-rules` (Atlas no es Flutter).

Manual:

```bash
.cursor/hooks/sync-skills.sh
```
