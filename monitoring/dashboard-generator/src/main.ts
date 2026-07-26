import { mkdirSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { buildAtlasOperationsCenter } from "./dashboards/atlasOperationsCenter.js";
import {
  assertNoDuplicatePanelIds,
  assertValidGrid,
  normalizeDashboard,
  toPrettyJson,
} from "./serialize.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const OUTPUT_PATH = path.resolve(
  __dirname,
  "../../dashboards/generated/atlas-operations-center.json",
);

function main(): void {
  const built = buildAtlasOperationsCenter().build();
  const dashboard = normalizeDashboard(built);

  assertValidGrid(dashboard);
  assertNoDuplicatePanelIds(dashboard);

  const json = toPrettyJson(dashboard);

  // Validate JSON round-trip.
  JSON.parse(json);

  mkdirSync(path.dirname(OUTPUT_PATH), { recursive: true });
  writeFileSync(OUTPUT_PATH, json, "utf8");

  console.log(`Wrote ${OUTPUT_PATH}`);
  console.log(`UID: ${String(dashboard.uid)}`);
  console.log(`schemaVersion: ${String(dashboard.schemaVersion)}`);
  console.log(`panels: ${Array.isArray(dashboard.panels) ? dashboard.panels.length : 0}`);
}

try {
  main();
} catch (error) {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode = 1;
}
