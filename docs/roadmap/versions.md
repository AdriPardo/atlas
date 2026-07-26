# Roadmap — v0.1 → v1.0

Cada versión es **usable** en producción self-hosted con el alcance declarado. No hay versiones “solo scaffolding”.

## Resumen

| Versión | Tema | Valor para el operador |
|---------|------|------------------------|
| **v0.1** | MVP actual endurecido | Inventario + SSO + CRUD |
| **v0.2** | Project/Service | Modelo mental comercial |
| **v0.3** | Jobs + worker skeleton | Base async |
| **v0.4** | Deploy real mínimo | Git + compose en host |
| **v0.5** | Runtime visibility | Containers, logs, metrics links |
| **v0.6** | Pipelines + webhooks | GitOps ligero |
| **v0.7** | RBAC + Network + Alerts | Multi-user serio |
| **v0.8** | Backups + Cron | Continuidad |
| **v0.9** | Billing usage + polish | Enterprise-ready metering |
| **v1.0** | GA | Producto comercial self-host |

---

## v0.1 — Foundation (ahora)

**Incluye:** JWT + Authentik SSO, CRUD Application/Host/Deployment (manual), Dashboard, Profile, Compose, Flyway V1–V4.

**Criterio done:** stack documentado, SSO en prod, tests verdes.

---

## v0.2 — Projects

- Migración Application → Project + Service.
- API `/projects` + `/services`; alias `/applications`.
- UI rename + redirects.
- Seed Organization (1 fila).

**Criterio done:** operador trabaja solo en “Projects”; datos MVP migrados.

---

## v0.3 — Job infrastructure

- Tabla `jobs`, claim `SKIP LOCKED`.
- Perfil/proceso worker (aunque handlers sean no-op o sync host stub).
- `POST /hosts/{id}/sync` encola job.
- Métricas básicas de cola.

**Criterio done:** job PENDING→RUNNING→SUCCEEDED visible en API.

---

## v0.4 — Real deploy path

- Adapters reales: `GitRepositoryPort`, `HostConnectorPort`, `ContainerRuntimePort` (Docker).
- Secrets + Variables modules (mínimo viable).
- `POST /services/{id}/deploy` → 202 → worker ejecuta → logs.
- Cancel/retry.
- Settings: URLs obs opcionales.

**Criterio done:** desplegar un compose real desde la UI a un host registrado.

---

## v0.5 — Observe the fleet

- Containers list por host.
- Log viewer (deploy + query Loki si configurado).
- Metrics deep-link Grafana + query Prom simple.
- Soft-delete projects; índices revisados.
- CI pipeline del repo Atlas.

**Criterio done:** diagnosticar un deploy fallido sin SSH manual.

---

## v0.6 — Pipelines

- Pipeline definition simple (deploy-centric).
- Git webhooks.
- Redis opcional (cache / pubsub logs).
- Live log improvements (SSE o poll eficiente).

**Criterio done:** push a branch dispara deploy automáticamente.

---

## v0.7 — Access & edge

- Teams + Permissions (VIEWER/DEVELOPER).
- Audit log UI.
- Domains + Certificates metadata + Traefik/Cloudflare adapters básicos.
- Alerts + Notification channels.

**Criterio done:** OPERATOR no-admin opera projects con ACL; dominio verificado.

---

## v0.8 — Resilience

- Backups/Restore volúmenes o DB registradas.
- Cron schedules.
- Job purge + retention policies.
- Hardening docs (runbooks).

**Criterio done:** backup programado + restore de prueba documentado.

---

## v0.9 — Commercial envelope

- Billing/usage meters + entitlements UI (precio puede ser 0).
- Feature flags / plan local.
- OpenAPI published; deprecations `/applications` removed if sunset elapsed.
- Performance pass (5k projects synthetic test).

**Criterio done:** informe de usage exportable; carga objetivo validada.

---

## v1.0 — GA

- Estabilidad, UX polish, docs de operación, upgrade path v0.x→v1.0.
- Plugin contract v1 (Cloudflare oficial).
- Security review checklist cumplido.
- AI Assistant **no** requerido (future).

**Criterio done:** release tag `v1.0.0`, changelog, imagen firmada/publicada.

---

## Fuera de v1.0 (explicitamente)

- Multi-tenant SaaS.
- Kubernetes first-class control plane.
- Marketplace completo.
- Kafka.
- Cobro Stripe obligatorio.
