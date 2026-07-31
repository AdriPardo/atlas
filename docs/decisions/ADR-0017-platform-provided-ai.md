# ADR-0017 — AI provista por la plataforma (no BYOK end-user)

- **Estado:** Accepted (contrato + mapeos envFrom; migración apps diferida)
- **Fecha:** 2026-07-31

## Contexto

Apps en Atlas (p. ej. Reelpath) usan proveedores de IA (OpenAI, DeepSeek, ElevenLabs, …). Hoy Reelpath guarda esas keys en **Secretos de plataforma** de la app (`PlatformSecret` en DB de la app) — UI orientada a usuario/org de producto, no a ops Atlas.

Eso choca con el modelo de producto:

- Atlas = plano de ops ([ADR-0002](ADR-0002-single-tenant-install.md)): **nosotros** proveemos infra y capacidades.
- End-user no debe pegar API keys de OpenAI/ElevenLabs; no es su responsabilidad ni su contrato.
- Mañana el proveedor puede ser un **modelo local** (OpenAI-compatible) sin que el usuario cambie nada: Autopilot / `atlas.yml` / secrets de Atlas poseen el swap.

Secrets + `envFrom` ya existen ([ADR-0014](ADR-0014-project-manifest-runtime.md), [ADR-0015](ADR-0015-project-database-access.md), [config-security.md](../modules/config-security.md)). Falta el contrato de producto para AI.

### Opciones evaluadas

| | Enfoque | Pros | Contras |
|---|---------|------|---------|
| **A** | BYOK end-user (status quo Reelpath PlatformSecret) | Flexibilidad por cliente | UX mala; coste/riesgo en usuario; swap local imposible sin reconfigurar cada tenant |
| **B** | Keys solo en host / Compose `.env` sin Atlas | Simple ops ad-hoc | Sin UI Atlas; sin resolución org→project; difícil rotación central |
| **C** | Secrets Atlas (org/project) + `envFrom` en `atlas.yml` | Reusa almacén; inject en deploy; swap provider sin tocar UI app | Apps deben leer env; UI BYOK a retirar/ocultar |

## Decisión

1. **AI es capacidad de plataforma**, no BYOK para end-users de apps hospedadas. Operadores Atlas (ADMIN/OPERATOR) gestionan keys; usuarios de Reelpath (y apps similares) **no** ven formularios de OpenAI/ElevenLabs/DeepSeek.
2. **Dónde viven las keys:** almacén de secrets Atlas — preferido **org/global** (una key compartida por install) o **project-owned** / binding si un project necesita override. Alternativa ops: env a nivel host (Compose del host) sin pasar por UI app; sigue siendo ops, no end-user.
3. **Entrega al runtime:** `atlas.yml` `runtime.envFrom` / `services.*.envFrom` → worker escribe `.env` antes de `compose up` (mismo path que `db.url`).
4. **Nombres lógicos (convención):**

   | Secret lógico | Env default | Uso |
   |---------------|-------------|-----|
   | `ai.openai` | `OPENAI_API_KEY` | Chat / embeddings OpenAI o compatible |
   | `ai.openai.base_url` | `OPENAI_BASE_URL` | Endpoint OpenAI-compatible (local / proxy) |
   | `ai.elevenlabs` | `ELEVENLABS_API_KEY` | TTS / voice |
   | `ai.deepseek` | `DEEPSEEK_API_KEY` | Provider DeepSeek (si la app lo usa aparte) |
   | `ai.provider` | `AI_PROVIDER` | Selector lógico (`openai` \| `deepseek` \| `local` \| …) |
   | `ai.api_key` | `AI_API_KEY` | Key genérica cuando la app abstrae un solo cliente |
   | `ai.base_url` | `AI_BASE_URL` | Base URL genérica (local / gateway) |

   **Multi-capability hoy (Reelpath):** preferir keys por vendor (`ai.openai` + `ai.elevenlabs` [+ `ai.deepseek`]).  
   **Single-client / swap futuro:** `ai.provider` + `ai.api_key` + opcional `ai.base_url` — Autopilot o ops cambian valores; la app no pide nada al usuario.
5. **Resolución:** binding alias → project-owned → org/global (igual que `git.token` / `db.url`).
6. **Apps existentes (Reelpath):** no borrar `PlatformSecret` en DB. Precedencia runtime: **env inyectado por Atlas gana** si está presente; fallback a PlatformSecret solo durante migración. UI de keys end-user: **ocultar detrás de flag ops** o retirar de pantallas de usuario; panel ops-only opcional hasta cutover.
7. **Atlas no es un LLM gateway** en este ADR: no proxya tokens ni factura AI por request. Solo posee secrets + inject. Metering AI = cola futura si billing lo pide.

## Fuera de alcance (ahora)

- Implementar gateway / rate-limit / quota de tokens en Atlas.
- Cambiar código Reelpath en este repo (repo app separado; ver nota de producto).
- Forzar un único vendor; apps multi-modelo siguen válidas vía secrets por vendor.
- UI “AI marketplace” o catálogo de modelos en Atlas.

## Consecuencias

- (+) Contrato claro: plataforma paga / opera AI; usuario consume feature, no keys.
- (+) Swap a local = cambiar `ai.openai.base_url` / `ai.base_url` (+ key dummy si hace falta) sin redeploy de UI ni touch de tenants.
- (+) Reusa secrets + envFrom; cero nuevo almacén.
- (−) Apps deben preferir `process.env` / config sobre PlatformSecret UI.
- (−) Migración Reelpath es trabajo en repo de la app + ops crea secrets org.

## Referencias

- Producto: [platform-provided-ai.md](../product/platform-provided-ai.md)
- Secrets / envFrom: [config-security.md](../modules/config-security.md)
- Manifiesto ejemplo: [atlas.project.example.yml](../schemas/atlas.project.example.yml)
