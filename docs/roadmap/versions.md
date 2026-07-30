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
| **v0.8.3** | Autopilot Proxmox guest-ready (slice 3b) | IP guest-agent + Host SSH/Sync + deploy en VM nueva |
| **v0.8.4** | Autopilot DNS CNAME (Cloudflare) | PUBLIC hostname resoluble vía CNAME API o copy |
| **v0.8.5** | Autopilot Proxmox REUSED | ISOLATED reutiliza Host/VM por hostname/tag; clone solo si no hay match |
| **v0.8.6** | Stale RUNNING job recovery | Redeploy tras crash del worker |
| **v0.8.7** | Auto-deploy on git push | One-click pipeline + filtro push/branch + GitHub webhook opcional |
| **v0.8.8** | Cloudflare token scopes in Secrets UI | Operador ve scopes mínimos Tunnel+DNS al crear el secret |
| **v0.8.9** | Read `atlas.yml` on deploy (ADR-0014 B) | Repo declara compose file; fallback `composePath` |
| **v0.8.10** | Optional `composePath` (ADR-0014 C) | Create/update sin path si hay manifiesto; error claro si falta ambos |
| **v0.8.11** | RuntimeOrchestratorPort (ADR-0014 D) | Deploy vía port genérico; Host `runtimeCapabilities`; Compose adapter |
| **v0.8.12** | Pipeline hostId optional + Autopilot webhook | Webhook/run sin pin; placement SHARED por run |
| **v0.8.13** | `runtime.migrateCommand` hook | App declara migrator; Atlas ejecuta post-compose |
| **v0.8.14** | Host capabilities DB + placement filter | SHARED solo hosts con `compose`; tags en DB |
| **v0.8.15** | UX Domains 403 scopes | Ensure Tunnel/DNS: 403 → mensaje scopes + link Secrets |
| **v0.8.16** | OpenAPI + `/applications` sunset path | Contrato publicado; alias deprecated hasta 2027-08-01 |
| **v0.8.17** | Host sync runtime capabilities | Sync escribe `compose`/`podman` desde probe; unreachable no pisa |
| **v0.8.18** | PUBLIC minify + TLS guarantees | ADR-0016: `NODE_ENV=production` + requireTls docs/edge |
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
**Estado:** backup job + cron + retención OK; runbook restore lógico + checklist en [backup-restore.md](../deployment/backup-restore.md).

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

## v0.8.3 — Autopilot Proxmox guest-ready (slice 3b)

- Clone → start → poll qemu-guest-agent (fallback `DEFAULT_GUEST_IP`).
- Host SSH + secret `proxmox.ssh.private_key` + `SYNC_HOST`; `DEPLOY_SERVICE` en esa VM.
- ADR-0012 actualizado.

**Criterio done:** ISOLATED con Proxmox + agent + SSH key deja el servicio RUNNING en la VM nueva.

---

## v0.8.4 — Autopilot DNS CNAME (Cloudflare)

- `DnsProviderPort.ensureCname` + adapter Cloudflare zone DNS.
- Deploy PUBLIC: Tunnel ensure + CNAME ensure; UI DNS / Ensure DNS.
- ADR-0013.

**Criterio done:** hostname PUBLIC resuelve (o copy CNAME) sin edición manual obligatoria en Zero Trust DNS.

---

## v0.8.5 — Autopilot Proxmox REUSED

- ISOLATED: reutiliza Host SSH existente por hostname `atlas-…`, o VM Proxmox por nombre/tag.
- Clone solo si no hay match y `ATLAS_PROXMOX_CLONE_ENABLED=true`.
- ADR-0012 actualizado; UI hint de placement.

**Criterio done:** redeploy ISOLATED no clona otra VM cuando ya existe Host/VM reutilizable; servicio RUNNING en ese Host.

---

## v0.8.6 — Stale RUNNING job recovery

- Heartbeat de lease + reclaim de jobs `RUNNING` huérfanos tras crash del worker (`ATLAS_JOB_STALE_TIMEOUT`).
- Cascade: deployment / service / project dejan de quedar en DEPLOYING/RUNNING eternos.
- Claim `SKIP LOCKED` de PENDING intacto.

**Criterio done:** tras matar/reiniciar el worker, un job stale se marca FAILED y un nuevo deploy puede encolarse y completar.

---

## v0.8.7 — Auto-deploy on git push

- `POST /pipelines/enable-auto-deploy`: asegura Pipeline por service (**sin** host pin por defecto; Autopilot en cada run) y registra webhook GitHub si hay `git.token` + `publicBaseUrl`.
- Webhook git: solo eventos `push` a la branch del service (ignora ping/PR/otras ramas/tags/deleted).
- UI: panel “Auto-deploy on push” en Project detail + instrucciones claras en Pipeline detail.

**Criterio done:** push a la branch del service redeployea sin crear pipeline/webhook a mano; otros eventos no encolan deploy.

---

## v0.8.8 — Cloudflare token scopes in Secrets UI

- Hint persistente en Org secrets y Project secrets: scopes mínimos para `cloudflare.api.token` (Zone DNS Edit + Tunnel / Cloudflare One Edit).
- HelperText dinámico al crear/vincular ese nombre.
- Docs alineados (`config-security`, public hostname).

**Criterio done:** operador ve en Secrets UI qué scopes necesita; Tunnel/DNS assist no falla por scopes mal documentados.

---

## v0.8.9 — Project manifest read on deploy (ADR-0014 phase B)

- Tras `cloneOrUpdate`, si el workspace tiene `atlas.yml` / `atlas.project.yml` válido → `runtime.composeFile` se pasa a `composeUp`.
- Sin manifiesto o sin `composeFile` → `Service.composePath` (fallback; columna/API intactas).
- `runtime.kind` omitido / `compose` / `podman-compose`; otros kinds fallan el deploy con mensaje claro.
- Schema de ejemplo y ADR alineados; sin rewrite del orchestrator.

**Criterio done:** deploy con `atlas.yml` usa ese compose file; repo sin manifiesto se comporta igual que antes.

---

## v0.8.10 — Optional composePath (ADR-0014 phase C)

- Create/update Project + Service: `composePath` opcional (`@Size` only); columna `services.compose_path` nullable (V18).
- Deploy: manifiesto `runtime.composeFile` → path; sin manifiesto → sintetiza manifiesto mínimo desde `composePath`; sin ambos → `DomainException` clara.
- UI: Runtime path opcional + hint `atlas.yml`; detalle muestra “from atlas.yml” si vacío.
- Sin eliminar columna ni renombrar `ContainerRuntimePort`.

**Criterio done:** crear service sin `composePath` + repo con `atlas.yml` deployable; legacy solo-`composePath` sigue verde.

---

## v0.8.11 — RuntimeOrchestratorPort (ADR-0014 phase D)

- `RuntimeOrchestratorPort.apply` / `teardown`; Compose adapter delega a `ContainerRuntimePort.composeUp` / `composeDown`.
- `ExecuteDeployServiceJobUseCase` deja de llamar `composeUp` directo; chequea Host `supportsRuntime(COMPOSE)`.
- Host API: `runtimeCapabilities` (derivado, hoy `["compose"]`); sin columna DB aún.
- Sin segundo runtime; sin eliminar `compose_path`.

**Criterio done:** deploy verde vía orchestrator; Host response incluye capabilities; inspect/logs/restart intactos.

---

## v0.8.12 — Pipeline hostId optional + Autopilot on webhook/run

- `Pipeline.hostId` nullable (Flyway V19); create/update DTOs sin `@NotNull` host.
- `enable-auto-deploy` crea pipeline **sin** pin de host (Reelpath-safe: Autopilot SHARED en cada push); `hostId` explícito sigue como override advanced.
- `RunPipeline` / git webhook pasan host null → `DeployServiceUseCase` → `AutopilotPlacementService.resolveHost`.
- UI: form Pipeline host en Advanced; list/detail muestran Autopilot si null.

**Criterio done:** webhook/auto-deploy sin host pin → placement por run; pin legacy sigue verde.

---

## v0.8.13 — App migrateCommand (ORM-agnostic)

- `runtime.migrateCommand` opcional en `atlas.yml`; deploy lo corre **después** de compose up vía `HostCommandPort`.
- Atlas **no** impone Prisma/Flyway; solo inyección env + comando declarado. Docs: [app-migrations.md](../deployment/app-migrations.md).
- Si el contenedor ya migra al start (p. ej. Reelpath), omitir el campo para evitar doble migrate.
- Sin wipe de DB; sin convertir customer apps a Flyway de Atlas.

**Criterio done:** manifiesto con `migrateCommand` → aparece en logs de deploy; sin campo → sin hook; tests verdes.

---

## v0.8.14 — Host runtimeCapabilities persisted + placement filter

- Flyway V20: `hosts.runtime_capabilities` JSONB default `["compose"]`.
- Domain/API: tags leídos desde DB (create sigue anunciando `compose`).
- `AutopilotPlacementService` SHARED: solo candidatos con `supportsRuntime(COMPOSE)`; vacío → seed `atlas-local`.
- Sin segundo runtime; sin write API de capabilities aún (sync futuro).

**Criterio done:** host sin `compose` no gana SHARED; host con `compose` sigue elegible; deploy compose verde.

---

## v0.8.15 — UX Domains 403 scopes

- Cloudflare Tunnel/DNS Ensure: HTTP **403** → `FAILED` con mensaje `token scopes insufficient` (+ scopes mínimos + hint Org/Project secrets).
- UI Domains: alerta warning + link a `/secrets` cuando el mensaje indica scopes; Publish no se presenta como éxito si Ensure falló.
- Otros status Cloudflare siguen mensaje opaco + copy fallback. Sin segundo runtime.

**Criterio done:** Ensure con token sin permisos → UI/API dice scopes; token correcto → Tunnel/DNS Apply/Already present verdes.

---

## v0.8.16 — OpenAPI published + `/applications` deprecation path

- Snapshot versionado: `docs/api/openapi.json` (+ `openapi.md`, regenerar con `ATLAS_WRITE_OPENAPI=true` / `-Datlas.writeOpenApi=true`).
- Contract test: paths canónicos + `/applications` marcado `deprecated` en OpenAPI.
- Path de retirada documentado: `docs/api/deprecations.md` (Sunset **2027-08-01**); headers `Deprecation`/`Sunset`/`Link` sin retirar alias.
- Live Swagger solo perfiles `local`/`docker`/`test` (sin cambio de security).

**Criterio done:** cliente externo puede consumir `openapi.json`; operador conoce successor `/projects`+`/services` y fecha Sunset.

---

## v0.8.17 — Host sync writes real runtimeCapabilities

- `HostConnectorPort.HostInspection` incluye tags detectados; probe soft Docker + Podman (local/SSH).
- `ExecuteSyncHostJobUseCase` reemplaza `runtime_capabilities` solo si el probe devolvió tags; unreachable / vacío → conserva tags previos (SHARED compose no se rompe por flapping).
- Docker presente → `compose`; Podman presente → `podman`; ambos coexisten. Sin adapter Podman de deploy aún.

**Criterio done:** sync con Docker → host anuncia `compose`; solo Podman → `podman` sin inventar compose; host offline mantiene capabilities anteriores.

---

## v0.8.18 — PUBLIC minify + TLS guarantees (ADR-0016)

- Manifiesto: `build.minify` / `exposure.requireTls` (default true).
- Deploy asegura `NODE_ENV=production` en `.env` del workspace (sin pisar otras keys).
- Política TLS PUBLIC: log edge Tunnel/Traefik `websecure`; encrypt = TLS in transit (no payload crypto).

**Criterio done:** repo sin campos usa defaults seguros; opt-out explícito documentado; Reelpath/SSO intactos.

---

## v0.9 — Commercial envelope

- **Hecho (slice meters):** usage_records + `BillingMeterPort`; `GET /billing/usage` + `/entitlements`; UI `/billing` + CSV; plan `community` precio 0; meter `deploy.count` en enqueue.
- **Hecho (envFrom inject):** manifiesto `envFrom.secretRef` → `.env` en deploy (`db.url` → `DATABASE_URL`); ADR-0015 delivery sin provisioner.
- **Hecho (performance 5k):** índice `lower(name)`; IT seed 5k + smoke list/search.
- **Hecho (DB provisioner slice 1):** CREATE ROLE/SCHEMA + grants; secrets `db.url`/`db.schema`; UI Database; `ATLAS_APP_DB_*` (DB `apps`, nunca `atlas`).
- Feature flags / plan local (pendiente endurecer).
- OpenAPI published; deprecations `/applications` removed if sunset elapsed.
- Credenciales TTL (ADR-0015 opción C) — **siguiente recomendado**.

**Criterio done:** informe de usage exportable ✅; carga objetivo validada ✅; entrega secret→Compose ✅; provisioner schema/rol ✅.

---

## v1.0 — GA

- Estabilidad, UX polish, docs de operación, upgrade path v0.x→v1.0.
- Autopilot placement maduro (reuse host + Proxmox provision + PUBLIC/INTERNAL).
- Dirección: manifiesto de proyecto + runtime pluggable ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)); Compose sigue siendo el adapter default.
- Plugin contract v1 (Cloudflare oficial).
- Security review checklist cumplido.
- AI Assistant **no** requerido (future).

**Criterio done:** release tag `v1.0.0`, changelog, imagen firmada/publicada.

---

## Fuera de v1.0 (explicitamente)

- Multi-tenant SaaS.
- Kubernetes first-class control plane (un adapter `runtime.kind: kubernetes` puede llegar después vía ADR-0014; no es el CP de Atlas).
- Marketplace completo.
- Kafka.
- Cobro Stripe obligatorio.
