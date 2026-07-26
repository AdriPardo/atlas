# Módulos — Platform

## Settings

Configuración de la instalación: URLs Grafana/Prometheus/Loki, feature flags, retention, SMTP, providers Cloudflare/Traefik, branding ligero.

Almacenamiento: fila Organization.settings JSON + claves tipadas en UI. Secretos de settings vía Secrets module.

## Billing (usage, no necesariamente cobro)

Aunque el producto sea self-hosted sin pasarela de pago, el módulo existe para:

- Medir usage: deploys, minutos de job, GB backup, # projects.
- Exponer reportes y límites soft (`plan` local: `community` | `enterprise` flag).
- Preparar licenciamiento comercial futuro sin reescribir dominio.

Entidades: `UsageRecord`, `PlanEntitlement`, `InvoiceStub` (opcional, generada localmente).

**No** integrar Stripe en v1.0 salvo demanda explícita; diseñar interfaces `BillingMeterPort`.

## Plugins

Contrato: JAR/classpath o process sidecar que implementa ports (p.ej. DNS provider). Registry en DB. v1.0: diseño + 0–1 plugin oficial (Cloudflare). Marketplace después.

## Marketplace

Catálogo de templates (compose stacks) y plugins firmados. future post-v1.

## AI Assistant

Chat contextual (docs + estado project) con tool-calling read-only primero. Requiere provider API key en Settings. future; no bloquea v1.0.
