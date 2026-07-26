# UX — Componentes y estados

## Componentes compartidos (sistema)

| Componente | Uso |
|------------|-----|
| `PageShell` | Padding/max-width consistente |
| `PageHeader` | Título, subtítulo, actions |
| `QueryState` | Loading / error / empty wrappers |
| `StatusChip` | Enum → color (deploy, host, service) |
| `DataTable` *(objetivo)* | Sort, filter, pagination server-side |
| `ResourceNotFound` | 404 de entidad |
| `ConfirmDialog` | Deletes, reveal secret, restore |
| `LogViewer` | Mono, autoscroll, search, download |
| `EmptyState` | CTA claro (“Create your first project”) |
| `ErrorState` | Mensaje + retry |
| `ForbiddenState` | 403 |

## Estados por vista de listado

1. **Loading** — skeleton o spinner (QueryState).
2. **Empty** — copy + primary CTA.
3. **Error** — mensaje API + Retry.
4. **Populated** — tabla/filas + filtros.
5. **Filtered empty** — “No matches” (no CTA create).

## Estados de detalle

- Header con StatusChip + actions contextuales (Deploy, Edit, Delete).
- Tabs si hay >3 secciones.
- Sub-recursos paginados (deployments del service).

## Formularios

- React Hook Form + Zod (ya en MVP).
- Validación inline; submit disabled mientras pending.
- Errores servidor mapeados a campos o banner.

## Feedback

- Snackbar éxito/error en mutaciones.
- Deploy: página de detalle con status en vivo (poll 2–5s o SSE v0.6+).

## Accesibilidad mínima

- Contraste tema light/dark MUI.
- Botones con labels; icon-only con Tooltip.
- Focus visible en drawer/dialogs.

## Responsive rules

- Tablas densas en desktop; en mobile columnas prioritarias (name, status, updated).
- No hover-only actions: menú `⋯` siempre.
