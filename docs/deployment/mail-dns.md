# DNS mail — entrega real a Gmail (`mail.atlasops.dev`)

Paso **8** del rollout SMTP platform (ADR-0018). Sin SPF/DKIM/DMARC, Atlas envía bien en LAN pero Gmail/Outlook pueden rechazar o mandar a spam.

Relacionado: [project-mail-access.md](../product/project-mail-access.md), [ADR-0018](../decisions/ADR-0018-project-mail-access.md).

## Contexto Atlas

| Pieza | Valor típico prod |
|-------|-------------------|
| VM platform | `192.168.1.24` (`/opt/atlas/atlas`) |
| MTA Compose | servicio `smtp` (`boky/postfix`) |
| From apps | `{project-slug}@mail.atlasops.dev` |
| Relay IP pública | `92.178.160.125` (egress Orange; confirmar en VM) |
| OpenDKIM | manual / host `atlas-smtp-1` (fuera del adapter Cloudflare de Atlas) |

Atlas **no** publica SPF/DKIM/DMARC por API hoy — `CloudflareDnsAdapter` solo hace CNAME de customer domains. Estos registros son **ops manual** en Cloudflare (o script curl con el mismo token `cloudflare.api.token`).

## 1. Pre-requisitos en la VM

En `/opt/atlas/atlas/.env` (prod):

```env
ATLAS_APP_SMTP_HOST=smtp
ATLAS_APP_SMTP_PORT=25
ATLAS_APP_SMTP_FROM_DOMAIN=mail.atlasops.dev
ATLAS_SMTP_RELAYHOST=          # vacío = entrega MX directa desde Postfix
ATLAS_SMTP_HOST_PORT=2525      # solo LAN; no abrir 25 a Internet
```

Reiniciar MTA tras cambios:

```bash
ssh -i ~/.ssh/atlas atlas@192.168.1.24
cd /opt/atlas/atlas
docker compose -f docker-compose.prod.yml up -d smtp
docker compose logs smtp --tail=50
```

## 2. Dónde crear registros

**Cloudflare Dashboard** → **atlasops.dev** → **DNS** → **Records** → Add record.

| Campo Cloudflare | Valor |
|------------------|-------|
| Proxy status | **DNS only** (gris) en A/MX/TXT mail — nunca naranja para SMTP |
| TTL | Auto o 300s |

> Registros en subdominio **`mail`**, no mezclar con SPF del apex (`atlasops.dev` hoy apunta a DonDominio).

## 3. Registros copy-paste

Sustituir `<SELECTOR>` y `<DKIM_PUBLIC>` tras leer la clave en el servidor (§4).

### A — hostname SMTP (HELO / coherencia)

| Type | Name | Content | Proxy |
|------|------|---------|-------|
| A | `mail` | `92.178.160.125` | DNS only |

### SPF

| Type | Name | Content |
|------|------|---------|
| TXT | `mail` | `v=spf1 ip4:92.178.160.125 -all` |

Si más IPs envían correo, añadir `ip4:x.x.x.x` antes de `-all`. Mantener **un solo** TXT SPF por nombre (`mail`).

### DKIM (OpenDKIM)

| Type | Name | Content |
|------|------|---------|
| TXT | `<SELECTOR>._domainkey.mail` | `v=DKIM1; k=rsa; p=<DKIM_PUBLIC>` |

Ejemplo nombre FQDN: `default._domainkey.mail.atlasops.dev`

Cloudflare acepta el valor en una línea; quitar saltos del `.txt` de OpenDKIM y unir `p=` sin espacios.

### DMARC (recomendado)

Empezar en monitor (`p=none`); subir a `quarantine` / `reject` cuando SPF+DKIM pasen.

| Type | Name | Content |
|------|------|---------|
| TXT | `_dmarc.mail` | `v=DMARC1; p=none; rua=mailto:dmarc-reports@atlasops.dev; adkim=s; aspf=s; pct=100` |

Alternativa en apex (afecta todo el dominio): name `_dmarc` con `rua=...` y alinear política con subdominios.

### MX (solo si recibís correo en `@mail.atlasops.dev`)

Para **solo envío transaccional**, MX **no es obligatorio**. Si queréis buzón:

| Type | Name | Content | Priority |
|------|------|---------|----------|
| MX | `mail` | `mail.atlasops.dev` | 10 |

## 4. Obtener selector y clave pública DKIM (SSH)

En el host que firma (VM platform o `atlas-smtp-1`):

```bash
ssh -i ~/.ssh/atlas atlas@192.168.1.24

# Selector configurado
sudo grep -E '^Selector|^Domain' /etc/opendkim.conf /etc/opendkim/*.conf 2>/dev/null

# Clave pública publicable (NO copiar el .private)
sudo find /etc/opendkim/keys -name '*.txt' 2>/dev/null
sudo cat /etc/opendkim/keys/mail.atlasops.dev/default.txt
# o dentro del contenedor si OpenDKIM va sidecar:
docker exec smtp find /etc/opendkim -name '*.txt' 2>/dev/null
```

Del fichero `.txt`, copiar solo la parte `p=...` (base64) al registro DNS. **Nunca** commitear ni pegar en chat la clave `.private`.

Postfix debe referenciar OpenDKIM (`milter` / `smtpd_milters`) — fuera del scope del Compose stock `boky/postfix`; ver ADR-0018.

## 5. Verificación

```bash
# SPF
dig TXT mail.atlasops.dev +short

# DKIM (sustituir selector)
dig TXT default._domainkey.mail.atlasops.dev +short

# DMARC
dig TXT _dmarc.mail.atlasops.dev +short

# A / MX
dig A mail.atlasops.dev +short
dig MX mail.atlasops.dev +short

# PTR egress (Gmail mira esto)
dig -x 92.178.160.125 +short
```

Herramientas online:

- [mail-tester.com](https://www.mail-tester.com/) — score + SPF/DKIM/DMARC
- [Google Admin Toolbox — Check MX](https://toolbox.googleapps.com/apps/checkmx/)
- [Google Admin Toolbox — Dig](https://toolbox.googleapps.com/apps/dig/)

Desde Atlas: Project → **Mail** → test send a Gmail; revisar cabeceras (`Authentication-Results`: `spf=pass`, `dkim=pass`, `dmarc=pass`).

## 6. Cloudflare API vs dashboard

| Método | Cuándo |
|--------|--------|
| **Dashboard manual** | Recomendado primera vez; 4–5 TXT/A fáciles de revisar |
| **API curl** | Automatizar o IaC; token en Org/Project secret `cloudflare.api.token` (scope **Zone → DNS → Edit**) |
| **Atlas Ensure DNS** | **No** — solo CNAME customer domains, no mail auth |

Ejemplo API (SPF):

```bash
ZONE_ID="<cloudflare-zone-id>"
TOKEN="<cloudflare.api.token>"

curl -sX POST "https://api.cloudflare.com/client/v4/zones/${ZONE_ID}/dns_records" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  --data '{"type":"TXT","name":"mail","content":"v=spf1 ip4:92.178.160.125 -all","ttl":300}'
```

`ZONE_ID`: Cloudflare → atlasops.dev → Overview → API (o `ATLAS_CF_ZONE_ID` en `.env`).

## 7. Problemas frecuentes

| Síntoma | Causa | Acción |
|---------|-------|--------|
| Gmail rechaza | Sin SPF/DKIM | Completar §3 |
| `spf=fail` | IP egress distinta a la del TXT | `curl -4 ifconfig.me` en VM; actualizar SPF |
| `dkim=fail` | DNS no publicado o Postfix sin milter | §4 + reiniciar opendkim/postfix |
| PTR genérico Orange | `*.dynamic.orange.es` | Pedir PTR fijo al ISP o usar `ATLAS_SMTP_RELAYHOST` (SendGrid/SES) |
| Proxy naranja en A mail | Cloudflare proxy SMTP | **DNS only** |
| SPF apex DonDominio | `include:spf.dondominio.com` en `atlasops.dev` | No sustituye SPF de `mail`; registros independientes |

## 8. Fallback relay

Si PTR dinámico impide inbox Gmail:

```env
ATLAS_SMTP_RELAYHOST=smtp.sendgrid.net:587
# + credenciales SendGrid; SPF/DKIM del proveedor
```

Ver [project-mail-access.md](../product/project-mail-access.md).
