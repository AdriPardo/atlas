# ADR-0017 — Secretos de usuario para apps (almacén + inject)

- **Estado:** Accepted (almacén + UI + envFrom; cutover apps opcional)
- **Fecha:** 2026-07-31
- **Aclaración:** este ADR **no** dice que Atlas sea proveedor de IA. Atlas **guarda e inyecta** secrets que el usuario/ops crea para sus aplicaciones (Reelpath, etc.).

## Contexto

Apps desplegadas en Atlas necesitan credenciales en runtime (API keys de terceros, tokens, connection strings, …). Sin un almacén en plataforma, esas keys viven en Compose `.env` ad-hoc, en UI de la app, o en git — malo para rotación, audit y multi-proyecto.

Atlas ya tiene:

- Secrets cifrados (org/global + project-owned) + bindings ([config-security.md](../modules/config-security.md))
- `envFrom.secretRef` en `atlas.yml` → worker escribe `.env` antes de `compose up` ([ADR-0014](ADR-0014-project-manifest-runtime.md), [ADR-0015](ADR-0015-project-database-access.md))

Falta el contrato de producto claro: **quién crea** los secrets y **cómo llegan** a la app.

### Opciones evaluadas

| | Enfoque | Pros | Contras |
|---|---------|------|---------|
| **A** | Solo keys en UI de cada app (p. ej. Reelpath `PlatformSecret`) | Familiar por app | Duplicado; sin resolución org→project; rotación ops débil |
| **B** | Solo `.env` en host / Compose sin Atlas | Simple ad-hoc | Sin UI; sin bindings; difícil audit |
| **C** | Secrets Atlas (org/project) + `envFrom` | UI create/rotate; inject en deploy; share vía binding | App debe leer env |

## Decisión

1. **Atlas es almacén + injector de secrets para apps**, no un gateway LLM ni un “AI provider”. El usuario (ADMIN/OPERATOR del project) crea secrets en la UI Atlas (o via API/script) y los referencia desde el repo.
2. **Flujo canónico:**
   1. Crear secret en **Project secrets** (o org + link).
   2. Declarar `runtime.envFrom` / `services.*.envFrom` en `atlas.yml`.
   3. Deploy materializa valores en `.env` del workspace → Compose / app.
3. **Quién gestiona:** OPERATOR+ en project-owned; ADMIN en org/global. Valores **nunca** se listan en claro ni van a logs de deploy.
4. **Nombres:** libres. Convenciones conocidas (`git.token`, `db.url`, `ai.openai`, …) tienen mapeo env por defecto; cualquier otro nombre → `SCREAMING_SNAKE`. Override con `env:` / `as:`.
5. **Resolución:** binding alias → project-owned → org/global (igual que Git/DB).
6. **Rotación:** UI “Rotate value” / `PUT` upsert / script seed. Redeploy para materializar.
7. **Apps con almacén propio (Reelpath):** no borrar datos existentes. Preferir env Atlas cuando esté presente; fallback al almacén de la app solo durante migración.
8. **Atlas no proxya ni factura tokens de IA** en este ADR. Si una app usa OpenAI/ElevenLabs, esas keys son **secrets del usuario** (o compartidos ops) inyectados como cualquier otro.

### Convención opcional `ai.*` (solo nombres lógicos)

| Secret lógico | Env default |
|---------------|-------------|
| `ai.openai` | `OPENAI_API_KEY` |
| `ai.openai.base_url` | `OPENAI_BASE_URL` |
| `ai.elevenlabs` | `ELEVENLABS_API_KEY` |
| `ai.deepseek` | `DEEPSEEK_API_KEY` |
| `ai.provider` | `AI_PROVIDER` |
| `ai.api_key` | `AI_API_KEY` |
| `ai.base_url` | `AI_BASE_URL` |

No implica que Atlas pague o opere el proveedor: son labels convenientes para keys que **el usuario** guarda en Atlas.

## Fuera de alcance (ahora)

- Gateway / rate-limit / quota de tokens en Atlas.
- Forzar un vendor de IA.
- UI “AI marketplace”.
- Cambiar código Reelpath en este repo (repo app separado).

## Consecuencias

- (+) Un flujo: UI Atlas → `envFrom` → runtime app.
- (+) Reusa almacén + bindings; cero nuevo storage.
- (+) Script seed sigue útil para bulk/ops; UI es el camino principal para secrets de app.
- (−) Apps deben leer `process.env` / config desde env inyectado.
- (−) Migración desde almacenes in-app es trabajo en el repo de la app.

## Referencias

- Producto: [secrets-for-apps.md](../product/secrets-for-apps.md) (sustituye el framing “platform-provided AI”)
- Secrets / envFrom: [config-security.md](../modules/config-security.md)
- Seed opcional: [seed-project-secrets.sh](../../scripts/seed-project-secrets.sh) + [env.secrets.example](../../scripts/env.secrets.example)
- Manifiesto ejemplo: [atlas.project.example.yml](../schemas/atlas.project.example.yml)
