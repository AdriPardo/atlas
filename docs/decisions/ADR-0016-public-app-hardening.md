# ADR-0016 — Garantías de plataforma para apps PUBLIC (minify + TLS)

- **Estado:** Accepted (contrato + slice deploy fino; post-build audit / HSTS diferidos)
- **Fecha:** 2026-07-30

## Contexto

Operadores piden que los fronts publicados por Atlas “se minifiquen” y que “las APIs se encripten”. Hay que fijar semántica de plataforma sin romper stacks existentes (p. ej. Reelpath) ni confundir cifrado de tránsito con crypto de payload.

### Interpretaciones evaluadas

| Pedido | Lectura plausible | Default Atlas |
|--------|-------------------|---------------|
| **Minify fronts** | Build de producción (`NODE_ENV=production`, vite/webpack minify) para SPA expuesto PUBLIC | **Sí** — plataforma empuja `NODE_ENV=production` en `.env` del workspace de deploy cuando `build.minify` no es `false` |
| Minify | Post-build check (bundle sin source maps / tamaño) | Diferido — hook/auditoría |
| Minify | Solo documentar convención en Dockerfile | Insuficiente solo |
| **Encrypt APIs** | **TLS en tránsito** (HTTPS only en edge PUBLIC: Traefik `websecure` + Cloudflare Tunnel HTTPS) | **Sí** — ya es path Autopilot; manifiesto declara `exposure.requireTls` (default `true`) |
| Encrypt | Payload encryption (app-level crypto de body) | **No** salvo demanda explícita — no es contrato de plataforma hoy |
| Encrypt | Secrets at-rest (AES / `ATLAS_SECRETS_MASTER_KEY`) | Ya existe; orthogonal a PUBLIC edge |
| Encrypt | HSTS / redirect HTTP→HTTPS en Traefik | Futuro endurecimiento; Tunnel ya termina HTTPS al cliente |

## Decisión

1. **“Encrypt” = TLS in transit** para exposición `PUBLIC`. Autopilot ya genera metadata Traefik con `entrypoints=websecure` y `tls=true` ([ADR-0011](ADR-0011-autopilot-tunnel-ingress.md), [ADR-0013](ADR-0013-autopilot-dns-cname.md)). El cliente habla HTTPS con Cloudflare; origen Tunnel → Traefik `:443`. Atlas **no** promete cifrado de payload HTTP aparte de TLS.
2. **“Minify” = production frontend build.** Atlas no parsea el bundler del repo; garantiza hint de entorno: si `build.minify` es true (default), el job de deploy asegura `NODE_ENV=production` en el `.env` del workspace (sin borrar otras keys). El Dockerfile / Compose del proyecto debe respetar ese env en `npm run build` / vite / webpack.
3. **Campos de manifiesto (`atlas.yml`):**

   | Campo | Default | Efecto |
   |-------|---------|--------|
   | `build.minify` | `true` | Asegura `NODE_ENV=production` en deploy |
   | `exposure.requireTls` | `true` | PUBLIC: log de garantía TLS + assert metadata Traefik `tls=true`; no abre HTTP plano en edge PUBLIC |

4. **INTERNAL** no exige TLS de mundo (LAN / entrypoint interno puede ser plain); `requireTls` aplica al path PUBLIC.
5. **No romper** deploys existentes: no fallar el job solo porque el repo omita los campos; defaults seguros. Opt-out explícito (`build.minify: false`) para debug local documentado — no recomendado en PUBLIC.

## Consecuencias

- (+) Contrato claro para operadores: minify vía build prod; encrypt = HTTPS.
- (+) Slice fino sin tocar Compose de apps cliente ni Reelpath auth.
- (−) Atlas no puede verificar minify del artefacto sin inspector post-build (cola).
- (−) Payload encryption / mTLS app-to-app quedan fuera de alcance.

## Fuera de alcance (ahora)

- Rechazar PUBLIC si Tunnel ensure queda MANUAL (sigue soft-assist).
- HSTS middleware Traefik global.
- Full payload crypto / envelope encryption de APIs.
- Forzar `NODE_ENV` dentro de contenedores que no lean `.env` del compose (convención del repo).
