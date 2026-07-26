# ADR-0003 — Authentik SSO + JWT Atlas

- **Estado:** Accepted
- **Fecha:** 2026-07-27

## Contexto

El edge ya usa Authentik ForwardAuth. Se necesita sesión de API para la SPA y autorización de dominio.

## Decisión

1. Confiar en cabeceras `X-authentik-*` **solo** detrás de Traefik ForwardAuth.
2. Endpoint `/api/v1/auth/sso` provisiona/actualiza User y emite **JWT propio Atlas**.
3. Login password permanece para local/dev.
4. Grupos Authentik → roles Atlas (ADMIN/OPERATOR hoy; extensible).

## Consecuencias

- (+) UX SSO sin OIDC complejo en SPA al inicio.
- (+) Un solo mecanismo Bearer en API.
- (−) Riesgo de spoofing si API queda expuesta sin middleware — mitigado por red interna + docs.
- (−) Más adelante puede añadirse OIDC Authorization Code como alternativa; no reemplaza JWT de sesión API de inmediato.
