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
  billing/           # read-only usage v1.0
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

1. Arranque: `GET /api/v1/auth/sso` → si 200, guardar JWT y entrar.
2. Si 401 SSO: mostrar login local (solo cuando Authentik off / local).
3. Requests: `Authorization: Bearer <token>`.
4. 401 global → logout / re-SSO.

## Performance

- Listados paginados; evitar fetch de logs/métricas en list views.
- Log viewer: streaming/chunked o ventanas temporales.
- Preferir rutas lazy (`React.lazy`) cuando el bundle crezca (v0.5+).

## Design system

Mantener MUI como base del producto interno. Tokens de marca Atlas (color, tipografía) en `theme.ts`. No rediseñar desde cero en cada módulo: extender patrones `PageShell` / `PageHeader` / `QueryState`.
