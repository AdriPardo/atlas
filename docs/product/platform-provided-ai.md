# Producto — AI provista por la plataforma

Contrato: apps en Atlas (Reelpath, …) **no** piden API keys de IA al end-user. Ops Atlas posee keys y las inyecta. Decisión: [ADR-0017](../decisions/ADR-0017-platform-provided-ai.md).

## Intento

Usuario usa features de IA (chat, voice, …). Plataforma elige provider (API de pago hoy, modelo local mañana). Cambio de provider = secrets / `atlas.yml` / Autopilot — **cero** reconfig por tenant en UI de la app.

## Dónde viven las keys

| Sitio | Quién | Cuándo |
|-------|-------|--------|
| Secret org/global Atlas (`ai.openai`, `ai.elevenlabs`, …) | ADMIN | Default install-wide |
| Secret project / binding | OPERATOR+ | Override por app |
| Env host / Compose (sin UI app) | Ops | Break-glass / pre-Atlas |

**No:** formularios end-user tipo “pega tu OpenAI key” en Reelpath u otras apps.

## Entrega

1. Ops crea secrets lógicos en Atlas (UI **Org secrets** / project).
2. Repo declara `envFrom.secretRef` en `atlas.yml`.
3. Deploy escribe env en `.env` del workspace → Compose.

Ejemplo multi-vendor:

```yaml
runtime:
  envFrom:
    - secretRef: db.url
    - secretRef: ai.openai          # → OPENAI_API_KEY
    - secretRef: ai.elevenlabs      # → ELEVENLABS_API_KEY
```

Swap local (OpenAI-compatible):

```yaml
runtime:
  envFrom:
    - secretRef: ai.openai          # key del gateway local o placeholder
    - secretRef: ai.openai.base_url # → OPENAI_BASE_URL (http://llm:11434/v1)
```

O abstracción single-client:

```yaml
runtime:
  envFrom:
    - secretRef: ai.provider        # → AI_PROVIDER
    - secretRef: ai.api_key         # → AI_API_KEY
    - secretRef: ai.base_url        # → AI_BASE_URL (opcional)
```

## Qué ve el usuario (Reelpath)

| Antes | Después |
|-------|---------|
| UI “Secretos de plataforma” pide OpenAI / DeepSeek / ElevenLabs | Esa UI **oculta** (flag ops) o solo visible a rol ops interno; end-user no la ve |
| Keys en `PlatformSecret` (DB app) | Preferir env Atlas; **no borrar** filas existentes en migración |
| Runtime lee solo PlatformSecret | Precedencia: env inyectado → fallback PlatformSecret (transición) |

## Migración Reelpath (fuera de este repo Atlas)

1. Ops: crear org secrets `ai.openai`, `ai.elevenlabs` (+ `ai.deepseek` si aplica); valores = keys actuales de producción.
2. Repo Reelpath: `atlas.yml` `envFrom` como arriba; Compose ya debe leer `OPENAI_API_KEY` / `ELEVENLABS_API_KEY` (o mapear).
3. Código: al resolver config AI, `process.env.*` primero; si vacío, PlatformSecret (compat).
4. UI: feature flag `PLATFORM_AI_SECRETS_UI=false` (default) — esconde form end-user; ops puede reactivar en emergencia.
5. Cutover: cuando env siempre presente en deploys Atlas, dejar de escribir nuevas keys en PlatformSecret; filas viejas quedan hasta purge ops explícito.

## Qué no hace Atlas (aún)

- Proxy LLM / billing por token.
- Catálogo de modelos en UI Atlas.
- Borrar datos de PlatformSecret en apps cliente.
