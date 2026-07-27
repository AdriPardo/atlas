package com.atlas.domain.deployment;

/**
 * Autopilot where-to-run policy (ADR-0012).
 *
 * <ul>
 *   <li>{@link #SHARED} — reuse a shared Docker host (LOCAL / fleet).
 *   <li>{@link #ISOLATED} — request a dedicated Proxmox VM; falls back to SHARED until provisioned.
 * </ul>
 */
public enum PlacementMode {
    SHARED,
    ISOLATED
}
