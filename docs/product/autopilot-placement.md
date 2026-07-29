# Autopilot Placement

## Product vision

The operator **connects an application** (repo + how-to-run). Atlas decides:

1. **Where** to run it (reuse a shared host vs provision a new VM).
2. **Whether** it is **world-accessible** (`PUBLIC`) or **internal-only** (`INTERNAL`).

**North star:** a project manifest (`atlas.yml`) is the source of truth for *how to run*; the runtime (Docker Compose today) is a pluggable adapter — see [ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md). Autopilot owns placement, exposure, secrets, and edge (Traefik / Tunnel / DNS); the manifest owns services, build, health, and runtime kind. **Phases B–D + v0.8.14 + v0.8.17:** deploy reads `runtime.composeFile` from `atlas.yml` when present; otherwise **repo + optional `composePath`** (synthesized in memory); stack apply goes through `RuntimeOrchestratorPort` (Compose adapter); Host persists `runtimeCapabilities`; **host sync** refreshes tags from Docker/Podman probe (keeps prior tags if unreachable); SHARED placement filters by capability.

The user configures as little as possible. Hosts, SSH, Sync, and Deploy remain the **execution substrate** — not the primary mental model.

## User journey (3–5 clicks)

1. **New Project** — name + Git repo (+ branch / compose path if non-default).
2. Open the project → primary CTA **Deploy**.
3. Choose exposure: **Public** (default) or **Internal**.
4. Choose placement: **Shared host** (default) or **Isolated VM**.
5. Confirm **Deploy** (no host picker required).
6. Land on the deployment detail / logs.

Optional (collapsed “Advanced”): override target host. Day-to-day operators should never need it.

## Decision tree

```text
Connect app → Deploy(exposure?, placementMode?)
        │
        ├─ Placement (ADR-0012)
        │     SHARED (default) → hosts with compose capability; LOCAL / atlas-local / online heuristic
        │     ISOLATED → VmProvisionerPort (Proxmox)
        │           CREATED/REUSED + IP → register Host SSH → use it
        │           else → fall back to SHARED LOCAL
        │
        ├─ Exposure
        │     PUBLIC  → ensure Domain stub + Traefik metadata (websecure / Tunnel path)
        │     INTERNAL → skip public Domain; LAN / private Traefik entrypoint only
        │
        └─ Enqueue existing DEPLOY_SERVICE job (Git clone + compose up)
```

### Reuse shared Docker host vs new VM (Proxmox)

| Decision | When | Status |
|----------|------|--------|
| **Reuse shared host** (`SHARED`) | Default | Auto-pick LOCAL / default host |
| **Provision new / reuse VM** (`ISOLATED`) | Isolation / capacity | Host o VM Proxmox por hostname/tag → `REUSED`; si no, clone + guest-agent (ADR-0012) |

### Exposure

| Mode | Edge | Domain / Traefik |
|------|------|------------------|
| `PUBLIC` | Traefik `websecure` + existing Cloudflare Tunnel / DNS path | Domain stub + Traefik label metadata |
| `INTERNAL` | Private / LAN entrypoint only | No public Domain record |

## How Host / Deploy / Jobs underpin Autopilot

Autopilot is a **policy layer** on top of the existing control plane — not a rewrite:

| Building block | Role under Autopilot |
|----------------|----------------------|
| **Host** | Placement target (LOCAL socket or SSH). Still created/synced; usually auto-selected, auto-seeded, or provisioned. |
| **Deployment** | Immutable record of “this service on this host”. |
| **Job `DEPLOY_SERVICE`** | Async worker path (clone + runtime apply). Unchanged contract; payload still carries `deploymentId` / `hostId`. |
| **Domain + Traefik metadata** | PUBLIC edge descriptor; INTERNAL skips public domain. |
| **VmProvisionerPort** | ISOLATED path → Proxmox clone / reuse (ADR-0012). |
| **Pipelines / webhooks** | `hostId` opcional; omitido → Autopilot placement en cada run (SHARED default). Pin advanced sigue válido. |

## Out of scope (explicit)

- Automatic Cloudflare DNS CNAME — done via ADR-0013 (API or copy block)
- Removing the Hosts UI/API (kept as Advanced)

## Success for the first slice

> Create a project → Deploy with exposure toggle → Atlas picks/creates a LOCAL host → `DEPLOY_SERVICE` runs → PUBLIC gets a Domain stub; INTERNAL does not.

## Follow-on

- Slice 2 — Tunnel: [ADR-0011](../decisions/ADR-0011-autopilot-tunnel-ingress.md)
- Slice 3 — Proxmox provisioner: [ADR-0012](../decisions/ADR-0012-autopilot-proxmox-provisioner.md)
- Slice 3b — guest ready (done): IP + Sync + deploy on new Host
- Slice 4 — DNS CNAME: [ADR-0013](../decisions/ADR-0013-autopilot-dns-cname.md) (done)
- Continuity — restore runbook (`docs/deployment/backup-restore.md`) (done)
- Proxmox VM reuse (`REUSED`) por hostname/tag (done)
- Later — project manifest / pluggable runtime: [ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)
- Residual — stale `RUNNING` jobs after worker crash (ops/recover path) — **done** (v0.8.6)
