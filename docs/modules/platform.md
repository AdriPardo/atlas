# Módulos — Platform

## Settings

Configuración de la instalación: URLs Grafana/Prometheus/Loki, feature flags (`GET /settings/features`), retention, SMTP, providers Cloudflare/Traefik, branding ligero.

Plan local: `ATLAS_PLAN_CODE=community|enterprise`. Flags: `enterprise` (derive del plan), `billing` (default on; `ATLAS_FEATURE_BILLING`), `audit_export` (on en enterprise; override `ATLAS_FEATURE_AUDIT_EXPORT`).

Almacenamiento: fila Organization.settings JSON + claves tipadas en UI. Secretos de settings vía Secrets module.

## Billing (usage, no necesariamente cobro)

Aunque el producto sea self-hosted sin pasarela de pago, el módulo existe para:

- Medir usage: deploys (`deploy.count`), job wall-time (`job.minutes`), backup size (`backup.gb`), gauges live de projects/hosts/deployments.
- Exponer reportes (`GET /billing/usage`, export CSV UI) y límites soft (`plan` local: `community` | `enterprise`).
- Preparar licenciamiento comercial futuro sin reescribir dominio.

Entidades: `UsageRecord`, `PlanEntitlement`. `InvoiceStub` diferido. Feature gate: flag `billing`.

**No** integrar Stripe en v1.0 salvo demanda explícita. Puerto: `BillingMeterPort` (adapter in-process). Soft metering: complete/fail/stale job → `job.minutes`; backup dump → `backup.gb`. Hard-enforce de soft limits aún no.

## Plugins

Contrato: JAR/classpath o process sidecar que implementa ports (p.ej. DNS provider). Registry en DB. v1.0: diseño + 0–1 plugin oficial (Cloudflare). Marketplace después.

## Marketplace

Catálogo de templates (compose stacks) y plugins firmados. future post-v1.

## AI Assistant

Chat contextual (docs + estado project) con tool-calling read-only primero. Requiere provider API key en Settings. future; no bloquea v1.0.
