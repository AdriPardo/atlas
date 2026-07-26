# Runbook — Añadir un servicio nuevo

**Estado:** plantilla (rutas reales TBD)

## Precondiciones

- [ ] Compose/stack path conocido en el host
- [ ] Red Docker objetivo conocida
- [ ] Política de exposición (interno / Traefik / Tunnel) definida
- [ ] Backup/restore del nuevo volumen definido

## Pasos

1. Crear directorio del servicio en el árbol de stacks (ruta TBD).
2. Añadir `compose.yml` con imagen pinneada (tag o digest).
3. Definir redes, volúmenes, healthcheck y límites de recursos.
4. Si es público: labels Traefik + entrada Tunnel (cuando existan configs verificadas).
5. Añadir scrape/logs si aplica (Prometheus/Alloy).
6. Desplegar: `docker compose up -d` en el stack correspondiente.
7. Verificar health + métricas + logs.
8. Crear ficha en `docs/services/<servicio>.md` con evidencia.
9. Actualizar `docs/operations/inventory.md`.

## Rollback

`docker compose down` del servicio nuevo; revertir cambios de Traefik/Tunnel/Prometheus.
