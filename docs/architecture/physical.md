# Arquitectura física

**Estado:** BLOQUEADO

## Evidencia requerida

| Dato | Fuente esperada | Estado |
| --- | --- | --- |
| Hostname / FQDN | `hostname`, DNS | Declarado: `192.168.1.35`, user `atlas` — **no alcanzable** desde el agente cloud |
| Proveedor / bare metal / VM | inventario host | BLOQUEADO |
| CPU / RAM / disco | `lscpu`, `free`, `df` | BLOQUEADO |
| Interfaces de red / IPs | `ip -br a` | Declarado LAN `192.168.1.35` (NO VERIFICADO en host) |
| Ubicación de datos | mounts, volúmenes Docker | BLOQUEADO |
| Sistema operativo | `/etc/os-release` | BLOQUEADO |

## Diagrama

No se dibuja un diagrama físico inventado. Tras la recolección:

```mermaid
flowchart LR
  Operator[Operador] -->|SSH declarado| Host["Host 192.168.1.35 user atlas"]
  CloudAgent[Cloud Agent AWS] -.->|BLOQUEADO reset SSH| Host
  Internet[Internet] -->|Túnel? TBD| Host
```

Actualizar con datos de `inventory/raw/host/` cuando el acceso funcione.
