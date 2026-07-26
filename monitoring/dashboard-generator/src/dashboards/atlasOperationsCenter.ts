import * as common from "@grafana/grafana-foundation-sdk/common";
import {
  DashboardBuilder,
  DashboardCursorSync,
  RowBuilder,
} from "@grafana/grafana-foundation-sdk/dashboard";
import { PanelBuilder as StatBuilder } from "@grafana/grafana-foundation-sdk/stat";
import { PanelBuilder as TextBuilder, TextMode } from "@grafana/grafana-foundation-sdk/text";
import * as units from "@grafana/grafana-foundation-sdk/units";
import { createAlertTablePanel } from "../components/alertTablePanel.js";
import { createLogPanel } from "../components/logPanel.js";
import { createQuickLinksPanel } from "../components/quickLinksPanel.js";
import { createNetworkThroughputPanel, createResourcePanel } from "../components/resourcePanel.js";
import { createServiceStatusPanel } from "../components/serviceStatusPanel.js";
import {
  createInstantStatPanel,
  instantPrometheusQuery,
  valueMappings,
} from "../components/statPanel.js";
import { PROMETHEUS } from "../config/datasources.js";
import {
  COLORS,
  cpuThresholds,
  criticalAlertsThresholds,
  filesystemThresholds,
  memoryThresholds,
  platformHealthThresholds,
  warningAlertsThresholds,
} from "../config/thresholds.js";

/**
 * Platform health PromQL.
 *
 * Do NOT use `> bool 0`: that always yields a series (0 or 1), so `or` never
 * falls through and WARNING would be masked by a CRITICAL=0 result.
 *
 * Using `count(...) > 0` (without bool) drops the series when false, so:
 * critical (2) > warning (1) > healthy (0).
 */
export const PLATFORM_HEALTH_EXPR = `
(
  count(ALERTS{alertstate="firing",severity="critical"}) > 0
) * 2
or
(
  count(ALERTS{alertstate="firing",severity="warning"}) > 0
) * 1
or
vector(0)
`
  .replace(/\s+/g, " ")
  .trim();

export const CRITICAL_ALERTS_EXPR =
  'count(ALERTS{alertstate="firing",severity="critical"}) or vector(0)';

export const WARNING_ALERTS_EXPR =
  'count(ALERTS{alertstate="firing",severity="warning"}) or vector(0)';

export const SERVICES_UP_EXPR = "count(up == 1)";
export const SERVICES_TOTAL_EXPR = "count(up)";

export const HOST_UPTIME_EXPR = "time() - max(node_boot_time_seconds)";

export const CPU_USAGE_EXPR = `
100 - (
  avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100
)
`
  .replace(/\s+/g, " ")
  .trim();

export const MEMORY_USAGE_EXPR = `
100 * (
  1 - (
    avg(node_memory_MemAvailable_bytes)
    /
    avg(node_memory_MemTotal_bytes)
  )
)
`
  .replace(/\s+/g, " ")
  .trim();

export const ROOT_FS_USAGE_EXPR = `
100 * (
  1 - (
    avg(
      node_filesystem_avail_bytes{
        mountpoint="/",
        fstype!~"tmpfs|overlay|squashfs"
      }
    )
    /
    avg(
      node_filesystem_size_bytes{
        mountpoint="/",
        fstype!~"tmpfs|overlay|squashfs"
      }
    )
  )
)
`
  .replace(/\s+/g, " ")
  .trim();

export const NETWORK_THROUGHPUT_EXPR = `
sum(
  rate(node_network_receive_bytes_total{
    device!~"lo|veth.*|br-.*|docker.*"
  }[5m])
)
+
sum(
  rate(node_network_transmit_bytes_total{
    device!~"lo|veth.*|br-.*|docker.*"
  }[5m])
)
`
  .replace(/\s+/g, " ")
  .trim();

function lastBackupPanel(): TextBuilder {
  return new TextBuilder()
    .title("Last Backup")
    .description("Backup telemetry has not yet been integrated into Prometheus.")
    .span(4)
    .height(5)
    .mode(TextMode.Markdown)
    .content(
      [
        '<div style="display:flex;align-items:center;justify-content:center;height:100%;">',
        '<span style="font-size:22px;font-weight:600;color:#9aa0a6;">NOT CONFIGURED</span>',
        "</div>",
        "",
        "_Backup telemetry has not yet been integrated into Prometheus._",
      ].join("\n"),
    );
}

/**
 * Services UP shows UP count as the primary value and TOTAL as a second field
 * (value + name), approximating the "8 / 8" operational readout.
 */
function servicesUpPanel(): StatBuilder {
  return new StatBuilder()
    .title("Services UP")
    .description(
      "Instant counts: query A = `count(up == 1)` (UP), query B = `count(up)` (TOTAL). Display shows both series (e.g. 8 and 8).",
    )
    .span(4)
    .height(5)
    .datasource(PROMETHEUS)
    .withTarget(instantPrometheusQuery(SERVICES_UP_EXPR, "A", "UP"))
    .withTarget(instantPrometheusQuery(SERVICES_TOTAL_EXPR, "B", "TOTAL"))
    .reduceOptions(
      new common.ReduceDataOptionsBuilder().calcs(["lastNotNull"]).values(true).limit(2),
    )
    .colorMode(common.BigValueColorMode.Background)
    .graphMode(common.BigValueGraphMode.None)
    .textMode(common.BigValueTextMode.ValueAndName)
    .noValue("0");
}

export function buildAtlasOperationsCenter(): DashboardBuilder {
  return new DashboardBuilder("Atlas Operations Center")
    .uid("atlas-operations-center")
    .tags(["atlas", "operations"])
    .editable()
    .timezone("browser")
    .refresh("30s")
    .time({ from: "now-6h", to: "now" })
    .tooltip(DashboardCursorSync.Tooltip)
    .withRow(new RowBuilder("Executive Status"))
    .withPanel(
      createInstantStatPanel({
        title: "Platform Health",
        description:
          "Derived from firing ALERTS: CRITICAL (2) > WARNING (1) > HEALTHY (0). Uses non-bool comparisons so `or` priority works in PromQL.",
        expr: PLATFORM_HEALTH_EXPR,
        span: 4,
        height: 5,
        decimals: 0,
        thresholds: platformHealthThresholds(),
        mappings: valueMappings([
          { value: "0", text: "HEALTHY", color: COLORS.green },
          { value: "1", text: "WARNING", color: COLORS.yellow },
          { value: "2", text: "CRITICAL", color: COLORS.red },
        ]),
        colorMode: common.BigValueColorMode.Background,
        graphMode: common.BigValueGraphMode.None,
        textMode: common.BigValueTextMode.Value,
      }),
    )
    .withPanel(
      createInstantStatPanel({
        title: "Critical Alerts",
        description: "Count of firing alerts with severity=critical.",
        expr: CRITICAL_ALERTS_EXPR,
        span: 4,
        height: 5,
        decimals: 0,
        thresholds: criticalAlertsThresholds(),
        colorMode: common.BigValueColorMode.Background,
        graphMode: common.BigValueGraphMode.None,
        noValue: "0",
      }),
    )
    .withPanel(
      createInstantStatPanel({
        title: "Warning Alerts",
        description: "Count of firing alerts with severity=warning.",
        expr: WARNING_ALERTS_EXPR,
        span: 4,
        height: 5,
        decimals: 0,
        thresholds: warningAlertsThresholds(),
        colorMode: common.BigValueColorMode.Background,
        graphMode: common.BigValueGraphMode.None,
        noValue: "0",
      }),
    )
    .withPanel(servicesUpPanel())
    .withPanel(
      createInstantStatPanel({
        title: "Host Uptime",
        description: "Time since last host boot from node_exporter (`node_boot_time_seconds`).",
        expr: HOST_UPTIME_EXPR,
        span: 4,
        height: 5,
        unit: units.DurationSeconds,
        decimals: 0,
        colorMode: common.BigValueColorMode.Value,
        graphMode: common.BigValueGraphMode.None,
      }),
    )
    .withPanel(lastBackupPanel())
    .withRow(new RowBuilder("Host Resources"))
    .withPanel(
      createResourcePanel({
        title: "CPU Usage",
        description: "Average non-idle CPU across all cores (5m rate).",
        expr: CPU_USAGE_EXPR,
        thresholds: cpuThresholds(),
      }),
    )
    .withPanel(
      createResourcePanel({
        title: "Memory Usage",
        description:
          "Percent of memory in use from MemAvailable/MemTotal (averaged if multiple series).",
        expr: MEMORY_USAGE_EXPR,
        thresholds: memoryThresholds(),
      }),
    )
    .withPanel(
      createResourcePanel({
        title: "Root Filesystem Usage",
        description: 'Usage of mountpoint "/" excluding tmpfs/overlay/squashfs.',
        expr: ROOT_FS_USAGE_EXPR,
        thresholds: filesystemThresholds(),
      }),
    )
    .withPanel(
      createNetworkThroughputPanel({
        title: "Network Throughput",
        description:
          "Sum of receive+transmit bytes/sec on non-virtual interfaces (excludes lo/veth/br/docker).",
        expr: NETWORK_THROUGHPUT_EXPR,
      }),
    )
    .withRow(new RowBuilder("Service Status"))
    .withPanel(createServiceStatusPanel(24, 10))
    .withRow(new RowBuilder("Alerts and Logs"))
    .withPanel(createAlertTablePanel(12, 10))
    .withPanel(createLogPanel(12, 10))
    .withRow(new RowBuilder("Quick Links"))
    .withPanel(createQuickLinksPanel(24, 5));
}
