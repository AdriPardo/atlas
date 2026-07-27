# ADR-0010 — Autopilot placement (platform decides host + exposure)

- **Estado:** Accepted
- **Fecha:** 2026-07-27

## Contexto

El modelo “el operador configura Hosts / SSH / Sync / Deploy” no encaja con la visión de producto: conectar una app y que la plataforma decida dónde corre y si es pública o interna. Ya existen Host, Deployment, Jobs (`DEPLOY_SERVICE`), Domains y metadata Traefik — no deben tirarse.

## Decisión

1. **Autopilot** es una capa de política sobre el control plane actual (no un rewrite).
2. `POST .../deploy` acepta `hostId` **opcional** y `exposure: PUBLIC | INTERNAL` (default `PUBLIC`).
3. Si no hay `hostId`, la plataforma selecciona un Host adecuado (preferir `LOCAL` online / `atlas-local` / `default`) o **crea** un Host LOCAL por defecto.
4. `exposure` se persiste en **Service**; `PUBLIC` asegura stub de Domain + metadata Traefik; `INTERNAL` no crea dominio público.
5. El job `DEPLOY_SERVICE` existente sigue siendo el ejecutor (Git + compose).
6. Provisionamiento Proxmox (VM nueva) y sync DNS Cloudflare real quedan para slices posteriores; Hosts UI permanece como Advanced.

## Consecuencias

- (+) Journey de 3–5 clics sin picker de host.
- (+) Infra prod (Proxmox/Docker/Traefik/Tunnel/Authentik) no se rompe: Autopilot reutiliza paths existentes.
- (+) Host/Deploy/Jobs siguen siendo la fuente de verdad operativa.
- (−) Hasta el provisioner Proxmox, “dónde” ≈ shared Docker host LOCAL (o override manual).
- (−) Tags de capacidad / scheduling rico aún no existen; la heurística es deliberadamente simple.
