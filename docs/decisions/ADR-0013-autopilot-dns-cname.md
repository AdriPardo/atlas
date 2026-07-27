# ADR-0013 — Autopilot PUBLIC DNS CNAME (Cloudflare)

- **Estado:** Accepted
- **Fecha:** 2026-07-27

## Contexto

Tras ADR-0011 (Tunnel Public Hostname), el hostname aún no resuelve en DNS público hasta crear un CNAME apuntando a `<tunnel-id>.cfargotunnel.com`. El token de Tunnel no implica Zone DNS Edit; pegar el record a mano cerraba el loop Autopilot.

## Decisión

1. Extender `DnsProviderPort` con `describeCname` / `ensureCname` (modos `APPLIED` | `UPDATED` | `ALREADY_PRESENT` | `MANUAL` | `SKIPPED` | `FAILED`).
2. `CloudflareDnsAdapter` upsert CNAME proxied vía API v4 (`GET` records → `POST` o `PATCH`) cuando existen `ATLAS_CF_ZONE` (y opcional `ATLAS_CF_ZONE_ID`), tunnel id (target) y secret `cloudflare.api.token`.
3. Sin credenciales → `MANUAL` con copy block (no falla el deploy).
4. Hostnames `*.local` / `*.atlas.local` → `SKIPPED`.
5. Tras `DEPLOY_SERVICE` PUBLIC succeeded: Tunnel ensure **y** DNS CNAME ensure; logs incluyen mode + copy si hace falta.
6. UI Domains: **DNS** (preview) / **Ensure DNS**. Ensure exitoso marca Domain `ACTIVE` si seguía `PENDING_DNS`.

### Scopes del token

| Secret / config | Scope Cloudflare recomendado |
|-----------------|------------------------------|
| `cloudflare.api.token` (compartido) | **Zone → DNS → Edit** + **Account → Cloudflare Tunnel → Edit** (o Cloudflare One) |
| `ATLAS_CF_ZONE` / `ATLAS_CF_ZONE_ID` | Zona donde viven los hostnames públicos |
| `ATLAS_CF_TUNNEL_ID` | Target CNAME = `{id}.cfargotunnel.com` |

Se puede usar un solo token con ambos permisos, o el mismo nombre de secret apuntando a un token DNS-only si Tunnel ensure se hace a mano.

## Consecuencias

- (+) Deploy PUBLIC puede dejar hostname resoluble sin editar DNS en el dashboard.
- (+) Misma superficie de secretos que Tunnel; fallback copy-ready.
- (−) Conflicto con record A/AAAA existente → `FAILED` (operador limpia o renombra).
- (−) Proxied CNAME asume edge Cloudflare; DNS-only no es el default Autopilot.
