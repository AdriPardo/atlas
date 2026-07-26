# ADR-005 — Cloudflare Tunnel

## Estado

Propuesta / **no verificada** (2026-07-26)

## Contexto

Briefing cita Cloudflare Tunnel. Config/token **NO ENCONTRADOS** en Git (correcto: no deben subirse secretos).

## Decisión (declarada)

Exponer servicios vía Cloudflare Tunnel en lugar de abrir puertos inbound.

## Motivación típica

- Reduce superficie de ataque en el edge del host
- TLS y DDoS gestionados en Cloudflare
- Encaja con Traefik como hop interno

## Evidencia requerida

Proceso/contenedor `cloudflared`, ingress rules (redactadas), conectividad.
