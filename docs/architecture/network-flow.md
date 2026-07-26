# Flujo de red

**Estado:** BLOQUEADO

## Preguntas abiertas (sin evidencia)

- ¿Qué redes Docker existen y cómo se segmentan?
- ¿Traefik escucha en qué entrypoints / puertos?
- ¿cloudflared termina TLS y reenvía a Traefik o a servicios?
- ¿Hay exposición directa de puertos en el host (`ss -tulpn`)?
- ¿Hay firewall (ufw/nftables/iptables)?

## Diagrama objetivo a completar

```mermaid
sequenceDiagram
  participant User as Cliente
  participant CF as Cloudflare
  participant Tunnel as cloudflared
  participant Traefik as Traefik
  participant Svc as Servicio

  Note over User,Svc: Flujo NO VERIFICADO
  User->>CF: HTTPS
  CF->>Tunnel: Tunnel
  Tunnel->>Traefik: HTTP interno?
  Traefik->>Svc: route
```

Referencias futuras: [../networking/README.md](../networking/README.md)
