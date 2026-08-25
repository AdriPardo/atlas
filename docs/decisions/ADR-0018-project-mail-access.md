# ADR-0018 — SMTP platform por Project

- **Estado:** Accepted
- **Fecha:** 2026-08-24

## Contexto

Apps desplegadas en Atlas (Reelpath, etc.) necesitan enviar email transaccional. Alertas de producto (`NotificationChannelType.EMAIL`) hoy son stub. Docs ya mencionaban SMTP en Settings sin implementación.

Patrón existente: ADR-0015 (DB por project) — provisioner platform + secrets `db.*` + `envFrom` en deploy.

## Decisión

1. **MTA propio** en la install: servicio Compose `smtp` (Postfix). Config `ATLAS_APP_SMTP_*`. Dev reenvía a Mailpit por defecto (`ATLAS_SMTP_RELAYHOST`).
2. **Auto en deploy:** si SMTP configurado, auto-provision secrets `smtp.*` (si faltan) + escribe `SMTP_*` en workspace `.env` (opt-out `ATLAS_APP_SMTP_AUTO_INJECT=false`).
3. **Provisioner manual** (`POST /projects/{id}/mail/provision`) sigue disponible.
4. **API HTTP** `POST /projects/{id}/mail/send` + rate limit.
5. **Alertas** usan el mismo MTA cuando host configurado.
6. **Billing:** meter `mail.send.count`.
7. **Prod:** Postfix con MX directo, o `ATLAS_SMTP_RELAYHOST` a upstream si hace falta deliverability.

## Fuera de alcance

- DKIM automation completa (ops DNS + firma en Postfix manual / sidecar).
- SMTP AUTH real por project en el MTA (hoy open-relay en red interna + API token).

## Consecuencias

- (+) Apps reciben SMTP en cada deploy sin tocar `atlas.yml`.
- (+) MTA propio en Compose; Mailpit solo captura local.
- (−) Apps fuera de `atlas-internal` necesitan `ATLAS_APP_SMTP_APP_HOST` alcanzable.
- → Producto: [project-mail-access.md](../product/project-mail-access.md).
