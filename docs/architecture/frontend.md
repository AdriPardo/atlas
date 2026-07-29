# Arquitectura — Frontend

## Forma actual (MVP)

```text
frontend/src/
  app/           # App, AppRouter, theme
  features/      # auth, dashboard, applications, hosts, deployments, profile
  shared/        # api client, layout, components (PageHeader, QueryState, StatusChip)
```

Stack: React, TypeScript, Vite, TanStack Query, MUI, React Hook Form, Zod.

## Principios UI

1. **Feature folders** por bounded context de producto (no por tipo técnico global).
2. **Server state** en TanStack Query; auth en contexto ligero.
3. **Rutas anidadas** bajo `AppLayout` (sidebar + app bar).
4. **Estados explícitos**: loading / empty / error / forbidden (ver [components-states.md](../ux/components-states.md)).
5. Evolución de copy: “Applications” → “Projects” con redirects (`/applications` → `/projects`) durante la transición.

## Estructura objetivo

```text
features/
  auth/
  dashboard/
  projects/          # ex applications
  services/          # detalle de servicio dentro de project
  deployments/
  pipelines/
  hosts/
  containers/
  logs/
  metrics/
  alerts/
  secrets/
  variables/
  domains/
  settings/
  teams/             # v0.7+
  audit/
  billing/           # usage + entitlements v0.9
  assistant/         # AI — late
shared/
  api/               # client, endpoints, error mapping
  layout/            # AppLayout, nav config por rol
  components/        # DataTable, ResourceHeader, StatusChip, LogViewer…
  auth/              # guards, role hooks
```

## Navegación

Config declarativa de sidebar (grupos: Overview, Delivery, Runtime, Network, Observability, Platform). Ítems aparecen según versión/feature flags y rol. Detalle en [information-architecture.md](../ux/information-architecture.md).

## Auth en SPA

1. Arranque: `GET /api/v1/auth/sso` (con reintentos en host público) → si 200, guardar JWT y entrar.
2. En `atlas.atlasops.dev`: no ofrecer login local; si SSO falla, CTA “Complete Authentik login” (re-ForwardAuth).
3. En localhost / IP LAN: login local + enlace al URL público SSO.
4. Requests: `Authorization: Bearer <token>`.
5. 401 global → logout / re-SSO.

El puerto host del frontend en prod debe ser loopback (`127.0.0.1:3000`) para no bypassear Traefik/Authentik desde la LAN.

## Performance

- Listados paginados; evitar fetch de logs/métricas en list views.
- Log viewer: streaming/chunked o ventanas temporales.
- Preferir rutas lazy (`React.lazy`) cuando el bundle crezca (v0.5+).

## Design system

Mantener MUI como base del producto interno. Tokens de marca Atlas (color, tipografía) en `theme.ts`. No rediseñar desde cero en cada módulo: extender patrones `PageShell` / `PageHeader` / `QueryState`.
