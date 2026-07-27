# UX — Information architecture

## Shell

- **App bar:** título de sección, toggle tema, usuario, logout.
- **Sidebar** (colapsable en mobile): navegación primaria por grupos.
- **Content:** `PageShell` + `PageHeader` (título, descripción, primary action).

## Sidebar (objetivo por fases)

### Grupo Overview
| Ítem | Ruta | Desde |
|------|------|-------|
| Dashboard | `/` | MVP |

### Grupo Delivery
| Ítem | Ruta | Desde |
|------|------|-------|
| Projects | `/projects` | v0.2 (alias `/applications`) |
| Deployments | `/deployments` | MVP |
| Pipelines | `/pipelines` | v0.6 |
| Repositories | `/repositories` | v0.5 |

### Grupo Runtime
| Ítem | Ruta | Desde |
|------|------|-------|
| Hosts | `/hosts` | MVP |
| Containers | `/containers` | v0.5 |
| Cron | `/cron` | v0.8 |

### Grupo Network
| Ítem | Ruta | Desde |
|------|------|-------|
| Domains | `/domains` | v0.7 |
| Certificates | `/certificates` | v0.7 |

### Grupo Observability
| Ítem | Ruta | Desde |
|------|------|-------|
| Logs | `/logs` | v0.5 |
| Metrics | `/metrics` | v0.5 |
| Alerts | `/alerts` | v0.7 |

### Grupo Platform
| Ítem | Ruta | Desde |
|------|------|-------|
| Secrets | `/secrets` | Organization secrets (ADMIN); project secrets on Project detail | v0.4 / v0.8 |
| Variables | `/variables` | v0.4 |
| Users / Teams | `/settings/users`, `/settings/teams` | v0.7 |
| Audit | `/audit` | v0.7 |
| Billing | `/billing` | v0.9 |
| Settings | `/settings` | v0.4 |
| Profile | `/profile` | MVP |

Ítems ocultos si feature flag off o rol insuficiente.

## Jerarquía de detalle Project

```text
/projects/:id
  ├─ Overview
  ├─ Services
  │    └─ /projects/:id/services/:serviceId
  │         ├─ Deployments
  │         ├─ Config (vars/secrets)
  │         ├─ Domains
  │         └─ Logs / Metrics
  ├─ Pipelines
  └─ Settings
```

## Dashboard layout

Una composición: salud + actividad. Sin “card farm” de marketing. KPIs escasos (4–6), tabla de deploys recientes, lista de issues (hosts offline / failed deploys).

## Responsive

- `< md`: drawer temporal; tablas → cards o scroll horizontal controlado.
- Acciones primarias siempre visibles en header o FAB contextual en mobile solo si necesario.
