import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { beforeAll, describe, expect, it } from "vitest";
import { buildAtlasOperationsCenter } from "../src/dashboards/atlasOperationsCenter.js";
import {
  TARGET_SCHEMA_VERSION,
  assertNoDuplicatePanelIds,
  assertValidGrid,
  collectPanelTitles,
  normalizeDashboard,
  toPrettyJson,
} from "../src/serialize.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const GENERATED_PATH = path.resolve(
  __dirname,
  "../../dashboards/generated/atlas-operations-center.json",
);

const REQUIRED_TITLES = [
  "Platform Health",
  "Critical Alerts",
  "Warning Alerts",
  "Services UP",
  "Host Uptime",
  "Last Backup",
  "CPU Usage",
  "Memory Usage",
  "Root Filesystem Usage",
  "Network Throughput",
  "Service Status",
  "Active Alerts",
  "Recent Warnings and Errors",
  "Quick Links",
];

describe("Atlas Operations Center dashboard", () => {
  let dashboard: Record<string, unknown>;
  let json: string;

  beforeAll(() => {
    const built = buildAtlasOperationsCenter().build();
    dashboard = normalizeDashboard(built);
    json = toPrettyJson(dashboard);
  });

  it("has fixed UID atlas-operations-center", () => {
    expect(dashboard.uid).toBe("atlas-operations-center");
  });

  it("has title Atlas Operations Center", () => {
    expect(dashboard.title).toBe("Atlas Operations Center");
  });

  it("uses schemaVersion 41", () => {
    expect(dashboard.schemaVersion).toBe(TARGET_SCHEMA_VERSION);
  });

  it("references Prometheus datasource UID prometheus", () => {
    expect(json).toContain('"uid": "prometheus"');
    expect(json).toContain('"type": "prometheus"');
  });

  it("references Loki datasource UID loki", () => {
    expect(json).toContain('"uid": "loki"');
    expect(json).toContain('"type": "loki"');
  });

  it("contains the main panels", () => {
    const titles = collectPanelTitles(dashboard);
    for (const title of REQUIRED_TITLES) {
      expect(titles).toContain(title);
    }
  });

  it("has valid gridPos without nulls, overflows, or overlaps", () => {
    expect(() => assertValidGrid(dashboard)).not.toThrow();
  });

  it("has no duplicate panel IDs", () => {
    expect(() => assertNoDuplicatePanelIds(dashboard)).not.toThrow();
  });

  it("produces valid JSON", () => {
    expect(() => JSON.parse(json)).not.toThrow();
  });

  it("writes a matching generated artifact when present", () => {
    const onDisk = readFileSync(GENERATED_PATH, "utf8");
    expect(() => JSON.parse(onDisk)).not.toThrow();
    const parsed = JSON.parse(onDisk) as Record<string, unknown>;
    expect(parsed.uid).toBe("atlas-operations-center");
    expect(parsed.schemaVersion).toBe(41);
  });
});
