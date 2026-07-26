# Estado de auditoría — Inventario Atlas

**Fecha (UTC):** 2026-07-26  
**Agente:** Cloud Agent `bc-ebb840c6-1285-45f6-9d17-854c2c7e9bb5`  
**Repo:** https://github.com/AdriPardo/atlas  
**Commit base auditado:** `bf519096a0fb07b68f74f7f36b8b2688a8342696` (`master`)

## Resultado ejecutivo

La misión de inventariar, documentar y organizar la infraestructura **no puede completarse sobre el servidor** con la evidencia disponible en este entorno.

Causa bloqueante: **no hay acceso SSH al host Atlas** ni configuración/código de infraestructura en el repositorio Git.

## Alcance inspeccionado (VERIFICADO)

| Ámbito | Resultado |
| --- | --- |
| Árbol Git en `/workspace` | Solo `README.md` con contenido `# atlas` |
| `docker-compose*.yml` / `compose.yml` en repo | **NO ENCONTRADO** |
| Configs Traefik / Prometheus / Grafana / Loki / Alloy | **NO ENCONTRADO** |
| Scripts, cron, systemd units en repo | **NO ENCONTRADO** |
| `~/.ssh` (claves / config) | **NO ENCONTRADO** |
| `SSH_AUTH_SOCK` / agente SSH usable | **NO ENCONTRADO** |
| Variables de entorno `ATLAS` / host remoto | **NO ENCONTRADO** |
| `.cursor/environment.json` / entorno cloud con secretos | **NO ENCONTRADO** (`environment: null`) |
| CLI Docker en el agente | **NO ENCONTRADO** |
| Worker privado / self-hosted | `usePrivateWorker: false` |

Evidencia: [`../../inventory/raw/`](../../inventory/raw/).

## Servicios mencionados en el briefing (NO VERIFICADO)

El briefing solicita inventariar: Traefik, Cloudflare Tunnel, Prometheus, Alertmanager, Grafana, Loki, Alloy, Node Exporter, cAdvisor.

**Ninguno de estos servicios aparece en el repositorio.** Sin SSH no se puede afirmar que existan, ni con qué imagen/versión/puertos/redes.

Fichas de servicio preparadas con estado bloqueado: [../services/README.md](../services/README.md).

## Qué sí se entregó en esta fase

- Estructura profesional `docs/` enlazada
- Inventario del estado real del repo y del entorno del agente
- Política de evidencia (no inventar)
- Script de recolección remota listo para ejecutar cuando exista SSH
- ADRs, runbooks y fichas de servicio en estado **pendiente de verificación**
- Informe de calidad y propuesta de reestructuración (sin aplicar cambios en el host)

## Actualización 2026-07-26 — intento SSH LAN

El operador indicó acceso `ssh atlas@192.168.1.35`.

Desde este Cloud Agent (egress AWS) el intento **falla**: reset en el intercambio de claves SSH, sin banner. La IP `192.168.1.35` es de LAN privada y no es operable desde el entorno cloud actual.

Ver evidencia y opciones: [access-requirements.md](access-requirements.md).

## Bloqueadores para Fase 2

1. Ruta de red alcanzable al host (private worker, Tailscale/CF Tunnel/bastion, o recolección local)
2. Autenticación SSH (clave en `authorized_keys` o mecanismo equivalente)

Ver [access-requirements.md](access-requirements.md).
