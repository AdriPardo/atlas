# Arquitectura lógica

**Estado:** NO VERIFICADO (plantilla del briefing)

El siguiente diagrama refleja **únicamente** los componentes citados en el briefing de inventario. No implica que estén desplegados ni conectados así.

```mermaid
flowchart TB
  CF[Cloudflare Tunnel] -->|NO VERIFICADO| TR[Traefik]
  TR --> Apps[Aplicaciones TBD]

  NE[Node Exporter] --> PROM[Prometheus]
  CAD[cAdvisor] --> PROM
  PROM --> AM[Alertmanager]
  PROM --> GRAF[Grafana]
  AL[Alloy] --> LOKI[Loki]
  LOKI --> GRAF
```

## Dependencias verificadas

Ninguna. En Git no hay compose ni labels Traefik.

## Acción

Sustituir este diagrama tras `docker inspect` + lectura de compose reales.
