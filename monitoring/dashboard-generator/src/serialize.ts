import type { Dashboard } from "@grafana/grafana-foundation-sdk/dashboard";

/** Grafana schemaVersion required by Atlas provisioning. */
export const TARGET_SCHEMA_VERSION = 41;

type JsonValue = null | boolean | number | string | JsonValue[] | { [k: string]: JsonValue };

/**
 * Normalize SDK output for deterministic provisioning JSON.
 * - Force schemaVersion 41
 * - Strip persistent panel `id` fields
 * - Drop undefined (JSON.stringify already does)
 */
export function normalizeDashboard(dashboard: Dashboard): Record<string, unknown> {
  const raw = JSON.parse(JSON.stringify(dashboard)) as Record<string, unknown>;
  raw.schemaVersion = TARGET_SCHEMA_VERSION;

  // Provisioned dashboards should not carry volatile Grafana-internal IDs.
  delete raw.id;
  delete raw.version;

  if (Array.isArray(raw.panels)) {
    raw.panels = raw.panels.map((panel) => stripPanelIds(panel as Record<string, unknown>));
  }

  return raw;
}

function stripPanelIds(panel: Record<string, unknown>): Record<string, unknown> {
  const next = { ...panel };
  delete next.id;
  if (Array.isArray(next.panels)) {
    next.panels = next.panels.map((child) => stripPanelIds(child as Record<string, unknown>));
  }
  return next;
}

export function toPrettyJson(dashboard: Record<string, unknown>): string {
  return `${JSON.stringify(dashboard, null, 2)}\n`;
}

export function assertValidGrid(dashboard: Record<string, unknown>): void {
  const panels = (dashboard.panels ?? []) as Array<Record<string, unknown>>;
  const occupied = new Map<string, string>();

  for (const panel of panels) {
    const type = String(panel.type ?? "");
    const title = String(panel.title ?? type);
    const grid = panel.gridPos as
      { x?: unknown; y?: unknown; w?: unknown; h?: unknown } | undefined;
    if (!grid) {
      if (type === "row") {
        continue;
      }
      throw new Error(`Panel "${title}" is missing gridPos`);
    }

    const x = Number(grid.x);
    const y = Number(grid.y);
    const w = Number(grid.w);
    const h = Number(grid.h);

    for (const key of ["x", "y", "w", "h"] as const) {
      const value = grid[key];
      if (value === null || value === undefined || Number.isNaN(Number(value))) {
        throw new Error(`Panel "${title}" has invalid gridPos.${key}=${String(value)}`);
      }
    }

    if (type === "row") {
      continue;
    }

    if (w < 1 || h < 1) {
      throw new Error(`Panel "${title}" has non-positive size w=${w} h=${h}`);
    }
    if (x < 0 || y < 0) {
      throw new Error(`Panel "${title}" has negative gridPos`);
    }
    if (x + w > 24) {
      throw new Error(`Panel "${title}" exceeds 24-column grid (x=${x}, w=${w})`);
    }

    for (let col = x; col < x + w; col += 1) {
      for (let row = y; row < y + h; row += 1) {
        const key = `${col},${row}`;
        const previous = occupied.get(key);
        if (previous) {
          throw new Error(`Panel "${title}" overlaps "${previous}" at ${key}`);
        }
        occupied.set(key, title);
      }
    }
  }
}

export function assertNoDuplicatePanelIds(dashboard: Record<string, unknown>): void {
  const panels = (dashboard.panels ?? []) as Array<Record<string, unknown>>;
  const seen = new Set<unknown>();
  for (const panel of panels) {
    if (!("id" in panel) || panel.id === undefined || panel.id === null) {
      continue;
    }
    if (seen.has(panel.id)) {
      throw new Error(`Duplicate panel id: ${String(panel.id)}`);
    }
    seen.add(panel.id);
  }
}

export function collectPanelTitles(dashboard: Record<string, unknown>): string[] {
  const panels = (dashboard.panels ?? []) as Array<Record<string, unknown>>;
  return panels.filter((p) => p.type !== "row").map((p) => String(p.title ?? ""));
}

// Keep JsonValue exported for tests if needed.
export type { JsonValue };
