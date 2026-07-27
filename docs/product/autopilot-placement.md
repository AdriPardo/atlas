# Autopilot Placement

## Product vision

The operator **connects an application** (repo + compose). Atlas decides:

1. **Where** to run it (reuse a shared Docker host vs provision a new VM).
2. **Whether** it is **world-accessible** (`PUBLIC`) or **internal-only** (`INTERNAL`).

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
        │     SHARED (default) → LOCAL / atlas-local / online heuristic
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
| **Provision new VM** (`ISOLATED`) | Isolation / capacity | Port + adapter wired; clone opt-in; guest-IP ready = next slice |

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
| **Job `DEPLOY_SERVICE`** | Async worker path (clone + compose). Unchanged contract; payload still carries `deploymentId` / `hostId`. |
| **Domain + Traefik metadata** | PUBLIC edge descriptor; INTERNAL skips public domain. |
| **VmProvisionerPort** | ISOLATED path → Proxmox clone / reuse (ADR-0012). |
| **Pipelines / webhooks** | May still pin a `hostId`; Autopilot deploy omits it. |

## Out of scope (explicit)

- Automatic Cloudflare DNS CNAME (copy block documents target; Tunnel Public Hostname is assisted via ADR-0011)
- Removing the Hosts UI/API (kept as Advanced)
- Full guest-agent wait / cloud-init IP discovery (next thin slice)

## Success for the first slice

> Create a project → Deploy with exposure toggle → Atlas picks/creates a LOCAL host → `DEPLOY_SERVICE` runs → PUBLIC gets a Domain stub; INTERNAL does not.

## Follow-on

- Slice 2 — Tunnel: [ADR-0011](../decisions/ADR-0011-autopilot-tunnel-ingress.md)
- Slice 3 — Proxmox provisioner: [ADR-0012](../decisions/ADR-0012-autopilot-proxmox-provisioner.md)
- Slice 3b — guest ready (IP + Sync + deploy on new Host)
