# Producto — Secretos de usuario para apps

Atlas guarda e inyecta **secrets que tú creas** para tus aplicaciones (Reelpath, etc.). No es un proveedor de IA: es el almacén cifrado + entrega en deploy. Decisión: [ADR-0017](../decisions/ADR-0017-platform-provided-ai.md).

> Nota: el fichero histórico `platform-provided-ai.md` apuntaba a “Atlas ofrece AI”. Ese framing quedó descartado; este doc es la fuente de verdad.

## Intento

Operador / miembro del project crea un secret en Atlas (API key de OpenAI, Stripe, Pexels, …). En el siguiente deploy, la app lo recibe como variable de entorno vía `envFrom` en `atlas.yml`.

## Flujo (UI)

1. Abre el **Project** → panel **Secrets**.
2. **Save** con nombre lógico (`openai.api_key`, `ai.openai`, `stripe.secret`, …) y valor.
3. En el repo, declara:

```yaml
runtime:
  envFrom:
    - secretRef: ai.openai          # → OPENAI_API_KEY (mapeo conocido)
    - secretRef: stripe.secret      # → STRIPE_SECRET (SCREAMING_SNAKE)
    - secretRef: custom.key
      env: MY_CUSTOM_ENV            # override explícito
```

4. **Deploy** el project. El worker escribe esas keys en el `.env` del workspace antes de `compose up`.
5. La app lee `process.env` / Compose interpolation. Redeploy tras rotar.

Alternativa: secret **org** (ADMIN) + **Link** al project con alias.

## Quién gestiona

| Ámbito | Rol | UI |
|--------|-----|-----|
| Project-owned | OPERATOR+ (`DEPLOY`) | Project → Secrets (create / rotate / delete) |
| Org/global | ADMIN | Sidebar **Org secrets** (create / rotate / delete) + link desde project |
| Bulk / CI | ADMIN token | `scripts/seed-project-secrets.sh` (opcional) |

Valores **nunca** se muestran en listados ni logs de deploy.

## Resolución en deploy

1. Binding del proyecto cuyo `alias` coincide  
2. Secret owned del proyecto con ese `name`  
3. Secret organization/global con ese `name`

## Relación con almacenes in-app (Reelpath)

Si la app aún tiene UI/`PlatformSecret` propia:

- Preferir env inyectado por Atlas cuando esté presente.
- No borrar filas existentes en migración.
- Ocultar o deprecar formularios de keys en la app cuando el flujo Atlas esté activo.

## Seed script (opcional)

Para cargar varios valores desde `.env.secrets` (gitignored) sin pegar en UI:

```bash
cp scripts/env.secrets.example .env.secrets
export ATLAS_ADMIN_USERNAME=... ATLAS_ADMIN_PASSWORD=...
./scripts/seed-project-secrets.sh
```

## Qué no hace Atlas (aún)

- Proxy LLM / billing por token.
- Catálogo de modelos.
- Borrar datos de almacenes in-app en clientes.
