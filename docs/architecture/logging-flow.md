# Flujo de logs

**Estado:** BLOQUEADO

Componentes citados: Alloy, Loki, Grafana.

## Evidencia en Git

**NO ENCONTRADO:** configs Alloy/Loki, pipelines, labels.

```mermaid
flowchart LR
  Sources[Contenedores / host] --> Alloy[Alloy]
  Alloy --> Loki[Loki]
  Loki --> Grafana[Grafana]
```

Ver: [../logging/README.md](../logging/README.md)
