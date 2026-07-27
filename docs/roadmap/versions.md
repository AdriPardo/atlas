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
| **v0.7.1** | Autopilot Placement (slice 1) | Connect app → Deploy; plataforma elige host + exposure |
| **v0.8** | Backups + Cron | Continuidad |
| **v0.8.1** | Autopilot Tunnel ingress (slice 2) | PUBLIC hostname: API o copy Zero Trust sin adivinar campos |
| **v0.8.2** | Autopilot Proxmox provisioner (slice 3) | SHARED vs ISOLATED cableado; Proxmox probe/clone opt-in |
| **v0.8.x** | Autopilot Proxmox guest-ready | IP real + Host Sync + deploy en VM nueva |
| **v0.9** | Billing usage + polish | Enterprise-ready metering |
| **v1.0** | GA | Producto comercial self-host + Autopilot path maduro |

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

## v0.7.1 — Autopilot Placement (slice 1)

- Deploy sin obligar `hostId`; auto-pick / seed Host LOCAL.
- `exposure` PUBLIC|INTERNAL en Service; Domain stub + Traefik metadata solo en PUBLIC.
- UI: CTA Deploy + toggle; Hosts como Advanced.
- ADR-0010 + `docs/product/autopilot-placement.md`.

**Criterio done:** Connect project → Deploy (3–5 clics) sin configurar Host manualmente.

**Camino a v1.0:** slice 2 provisiona VMs Proxmox reutilizando Host + `DEPLOY_SERVICE` (sin tirar el control plane).

---

## v0.8 — Resilience

- Backups/Restore volúmenes o DB registradas.
- Cron schedules.
- Job purge + retention policies.
- Hardening docs (runbooks).

**Criterio done:** backup programado + restore de prueba documentado.

---

## v0.8.1 — Autopilot Tunnel ingress (slice 2)

- `CloudflareTunnelPort` + copy/ensure Zero Trust Public Hostname.
- ADR-0011.

**Criterio done:** PUBLIC hostname asistido sin adivinar campos Zero Trust.

---

## v0.8.2 — Autopilot Proxmox provisioner (slice 3)

- `VmProvisionerPort` + adapter Proxmox; `placementMode` SHARED|ISOLATED.
- Fallback LOCAL hasta clone+guest IP; ADR-0012.

**Criterio done:** decisión isolated cableada; config/secret surface lista; deploy no se rompe sin Proxmox.

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
- Autopilot placement maduro (reuse host + Proxmox provision + PUBLIC/INTERNAL).
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
