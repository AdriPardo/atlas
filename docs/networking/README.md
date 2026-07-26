# Networking

**Estado:** BLOQUEADO

## Ámbitos a inventariar en host

| Ámbito | Comando / fuente | Estado |
| --- | --- | --- |
| Redes Docker | `docker network ls/inspect` | BLOQUEADO |
| Puertos en escucha | `ss -tulpn` | BLOQUEADO |
| Traefik entrypoints/routers | config dinámica/estática | BLOQUEADO |
| Cloudflare Tunnel ingress | config cloudflared | BLOQUEADO |
| Firewall | ufw/nftables | BLOQUEADO |

## Enlaces

- [../architecture/network-flow.md](../architecture/network-flow.md)
- [../services/traefik.md](../services/traefik.md)
- [../services/cloudflare-tunnel.md](../services/cloudflare-tunnel.md)
