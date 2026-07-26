# Requisitos de acceso para completar el inventario

Sin acceso interactivo real al host, cualquier ficha de servicio o diagrama de runtime sería especulación.

## Datos proporcionados por el operador

| Campo | Valor | Estado |
| --- | --- | --- |
| Usuario SSH | `atlas` | declarado por operador (2026-07-26) |
| Host | `192.168.1.35` | declarado por operador |
| Puerto | `22` (probado) | TCP sin banner SSH usable desde el agente |

## Prueba desde este Cloud Agent (VERIFICADO)

Evidencia: [`../../inventory/raw/ssh-probes/20260726-lan-reachability.txt`](../../inventory/raw/ssh-probes/20260726-lan-reachability.txt)

| Comprobación | Resultado |
| --- | --- |
| Egress del agente | IP pública AWS (`api.ipify.org` ≈ `54.187.34.250`) |
| `ssh atlas@192.168.1.35` | `kex_exchange_identification: Connection reset by peer` |
| Banner SSH | vacío (servidor no completa handshake) |
| HTTP 80/443/8080/3000/9090 | sin respuesta útil |
| Conclusión | **La LAN `192.168.1.0/24` no es alcanzable de forma útil desde este agente cloud** |

Nota: `/dev/tcp` marca “OPEN” incluso a `8.8.8.8:9`; es un falso positivo de red (accept/reset), no conectividad real al homelab.

## Opciones para desbloquear Fase 2

Cualquiera basta:

| Opción | Qué hace falta |
| --- | --- |
| **A — Private worker** | Ejecutar el Cloud Agent / worker self-hosted **dentro** de la LAN Atlas (o en el propio `192.168.1.35`) |
| **B — Túnel de gestión** | Exponer SSH vía Cloudflare Tunnel (TCP), Tailscale, WireGuard o bastion público; dar host/puerto alcanzable desde Internet |
| **C — Recolección local** | En el host: clonar la rama y ejecutar `./scripts/inventory/collect-host-inventory.sh --local`, luego commit de `inventory/raw/host/` (sin secretos) |
| **D — Clave + ruta pública** | Si existe bastion: añadir la pubkey del agente a `authorized_keys` y comunicar host/puerto público |

### Pubkey del agente (si se usa opción B/D)

```
ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIKA0oMx7S2LPxiQ2tgF06LChmLI+pPmnM9EqGChqCkaR cursor-atlas-inventory-agent
```

> Esta clave vive solo en el filesystem efímero del agente. Tras reinicio del entorno puede regenerarse; conviene un secret persistente o worker privado.

## Qué se ejecutará cuando el SSH funcione de verdad

```bash
export ATLAS_SSH_HOST="<host-alcanzable>"
export ATLAS_SSH_USER="atlas"
# export ATLAS_SSH_KEY=/path/to/key   # si aplica
./scripts/inventory/collect-host-inventory.sh --remote --out inventory/raw/host
```

Detalle: [host-collection.md](host-collection.md)
