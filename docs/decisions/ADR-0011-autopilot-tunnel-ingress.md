# ADR-0011 — Autopilot PUBLIC Tunnel ingress assist

- **Estado:** Accepted
- **Fecha:** 2026-07-27

## Contexto

Tras Autopilot slice 1, un deploy `PUBLIC` crea Domain + labels Traefik, pero el hostname solo es reachable cuando Cloudflare Tunnel tiene un **Public Hostname** (ingress remoto). El token DNS no basta; el dolor real (Reelpath) era pegar campos a mano en Zero Trust.

## Decisión

1. Puerto hexagonal `CloudflareTunnelPort`: siempre genera un `TunnelIngressSpec` copy-ready (subdomain, zone, HTTPS → `traefik:443`, No TLS Verify, CNAME hint).
2. `ensurePublicHostname` intenta API Zero Trust (`GET`+`PUT` `/accounts/{id}/cfd_tunnel/{id}/configurations`) **solo si** existen `ATLAS_CF_ACCOUNT_ID`, `ATLAS_CF_TUNNEL_ID` y secret `cloudflare.api.token`. Si faltan → modo `MANUAL` (no falla el deploy).
3. Hostnames `*.local` / `*.atlas.local` → `SKIPPED`.
4. Tras `DEPLOY_SERVICE` succeeded con exposure PUBLIC, el worker llama ensure y deja el bloque en los logs.
5. UI Domains: botones **Tunnel** (preview + copy) y **Ensure** (API o fallback).

## Consecuencias

- (+) Menos clics Zero Trust; Autopilot se acerca a “connect → expose”.
- (+) Sin credenciales, el operador sigue teniendo los valores exactos (no adivinar).
- (−) DNS CNAME automático: ver [ADR-0013](./ADR-0013-autopilot-dns-cname.md) (token Zone DNS Edit).
- (−) PUT de tunnel config es full-replace de ingress: el adapter hace merge GET→PUT y preserva catch-all 404.
