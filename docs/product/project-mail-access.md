# Producto — Mail por Project (SMTP propio)

Contrato SMTP platform. Decisión: [ADR-0018](../decisions/ADR-0018-project-mail-access.md).

## Intento

Atlas corre **su propio MTA** (Postfix en Compose). Cualquier app que se despliega recibe `SMTP_*` en el `.env` del workspace **sin** declarar `envFrom` ni pulsar Provision.

## Arquitectura local

```
App contenedor ──SMTP──► smtp (Postfix :25)
                              │
                              ▼ (dev default)
                         mailpit :1025  → UI :8025
```

- **smtp** — MTA propio (`boky/postfix`). Hostname Docker: `smtp`.
- **mailpit** — bandeja de captura en dev (Postfix reenvía aquí por defecto).
- **Prod** — vaciar `ATLAS_SMTP_RELAYHOST` (entrega MX directa) o apuntar a SES/SendGrid.

## Flujo (automático)

1. `docker compose up` levanta `smtp` + `mailpit` + backend con `ATLAS_APP_SMTP_HOST=smtp`.
2. **Deploy** de cualquier project:
   - Si faltan secrets `smtp.*` → auto-provision
   - Escribe en `.env`: `SMTP_HOST`, `SMTP_PORT`, `SMTP_FROM`, `MAIL_FROM`, `SMTP_TLS`, `SMTP_USER`, `SMTP_PASSWORD`
3. App lee `process.env.SMTP_*` / Compose interpolation.

Opt-out: `ATLAS_APP_SMTP_AUTO_INJECT=false`.

## Variables install

| Var | Dev default | Notas |
|-----|-------------|-------|
| `ATLAS_APP_SMTP_HOST` | `smtp` | Host que usa Atlas backend |
| `ATLAS_APP_SMTP_APP_HOST` | (vacío = mismo) | Host que ven las apps; p.ej. `host.docker.internal` si no están en `atlas-internal` |
| `ATLAS_APP_SMTP_PORT` | `25` | Puerto del MTA |
| `ATLAS_APP_SMTP_FROM_DOMAIN` | `mail.atlas.local` | From = `{slug}@{domain}` |
| `ATLAS_SMTP_RELAYHOST` | `[mailpit]:1025` | Vacío = entrega directa; o `smtp.sendgrid.net:587` |
| `ATLAS_SMTP_HOST_PORT` | `2525` | Publish host→contenedor 25 |

## Red de apps

Apps en la misma red Docker `atlas-internal` resuelven `smtp` sin más.

Si el compose de la app **no** está en esa red:

```env
ATLAS_APP_SMTP_APP_HOST=host.docker.internal
ATLAS_APP_SMTP_PORT=2525
```

(o IP LAN del host Atlas).

## Prod (SMTP propio real)

```env
ATLAS_APP_SMTP_HOST=smtp
ATLAS_APP_SMTP_PORT=25
ATLAS_APP_SMTP_FROM_DOMAIN=mail.tudominio.com
ATLAS_SMTP_RELAYHOST=          # vacío = MX directo
# DNS: SPF/DKIM/DMARC en mail.tudominio.com; abrir egress 25 si hace falta
```

Opcional relay solo si reputación IP falla:

```env
ATLAS_SMTP_RELAYHOST=smtp.sendgrid.net:587
```

## API / UI

- Panel Project → **Mail** (status + test send)
- `POST /projects/{id}/mail/send` sigue disponible
- `GET /settings/mail`

## Qué no hacer

- No exponer puerto 25 a Internet abierto (open relay). Solo red interna / firewall.
- No spam bulk.
- No confundir con email Authentik (SSO).
