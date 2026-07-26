# ADR-002 — Traefik como reverse proxy

## Estado

Propuesta / **no verificada** (2026-07-26)

## Contexto

Se requiere un edge router para enrutar tráfico a servicios. El briefing cita Traefik. **NO ENCONTRADO** en Git.

## Decisión (declarada)

Traefik como reverse proxy interno (típicamente detrás de Cloudflare Tunnel).

## Motivación típica (no atribuida a archivos Atlas)

- Descubrimiento por labels Docker
- Routing HTTP dinámico
- Integración habitual con túneles y TLS

## Evidencia requerida

Compose/config Traefik, entrypoints, routers, middlewares, métricas.
