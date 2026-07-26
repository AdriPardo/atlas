# Runbook — Recuperar Traefik

**Estado:** BLOQUEADO

## Impacto potencial

Pérdida de routing interno/público (si el Tunnel apunta a Traefik).

## Pasos genéricos

1. Estado del contenedor Traefik + cloudflared.
2. Validar config estática/dinámica y permisos del socket Docker (si usa provider Docker).
3. Comprobar redes Docker de Traefik vs backends.
4. Reinicio Traefik; verificar routers.
5. Smoke test de hosts internos/públicos.

## Evidencia requerida

Compose, entrypoints, archivo de config, dependencia del Tunnel.
