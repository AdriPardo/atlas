# Producto — AI provista por la plataforma

Contrato: apps en Atlas (Reelpath, …) **no** piden API keys de IA al end-user. Ops Atlas posee keys y las inyecta. Decisión: [ADR-0017](../decisions/ADR-0017-platform-provided-ai.md).

## Intento

Usuario usa features de IA (chat, voice, …). Plataforma elige provider (API de pago hoy, modelo local mañana). Cambio de provider = secrets / `atlas.yml` / Autopilot — **cero** reconfig por tenant en UI de la app.

## Cómo se guardan (ops)

| Quién | Qué | Cómo |
|-------|-----|------|
| Ops | Seed / rotate | `./scripts/seed-project-secrets.sh` lee `.env.secrets` (gitignored) o env VM → `PUT` Atlas secrets |
| End-user | Nunca | No UI de pegar OpenAI/ElevenLabs |
| UI Atlas Org/Project secrets | Break-glass | No es el flujo preferido; script es canónico |

Una vez / en cada rotación. Redeploy para materializar `envFrom` en `.env` del workspace.

```bash
cp scripts/env.secrets.example .env.secrets   # rellenar; no commit
export ATLAS_ADMIN_USERNAME=... ATLAS_ADMIN_PASSWORD=...
# default: org/global secrets
./scripts/seed-project-secrets.sh

# project-owned:
# ATLAS_SECRET_SCOPE=project ATLAS_PROJECT_ID=<uuid> ./scripts/seed-project-secrets.sh

# migración Reelpath (también PlatformSecret en DB app):
# REELPATH_SEED_PLATFORM=1 ./scripts/seed-project-secrets.sh
```

## Dónde viven las keys

| Sitio | Quién | Cuándo |
|-------|-------|--------|
| Secret org/global Atlas (`ai.openai`, `ai.elevenlabs`, …) | ADMIN vía script | Default install-wide |
| Secret project / binding | OPERATOR+ vía script | Override por app |
| Env host / Compose (sin UI app) | Ops | Break-glass / pre-Atlas |
| Reelpath `PlatformSecret` | Script bridge opcional | Solo migración; no UI end-user |

**No:** formularios end-user tipo “pega tu OpenAI key” en Reelpath u otras apps.

## Entrega

1. Ops corre seed script (Atlas secrets).
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

## Migración Reelpath (repo app + ops)

1. Ops: `seed-project-secrets.sh` con keys actuales (org o project Reelpath). Opcional `REELPATH_SEED_PLATFORM=1` para upsert `PlatformSecret` sin UI.
2. Repo Reelpath: `atlas.yml` `envFrom` `ai.openai` / `ai.elevenlabs` / …; Compose lee esas env keys.
3. Código: al resolver config AI, `process.env.*` primero; si vacío, PlatformSecret (compat).
4. UI: feature flag `PLATFORM_AI_SECRETS_UI=false` (default) — esconde form end-user.
5. Cutover: cuando env siempre presente en deploys Atlas, dejar de escribir nuevas keys en PlatformSecret; filas viejas quedan hasta purge ops explícito.

## Qué no hace Atlas (aún)

- Proxy LLM / billing por token.
- Catálogo de modelos en UI Atlas.
- Borrar datos de PlatformSecret en apps cliente.
