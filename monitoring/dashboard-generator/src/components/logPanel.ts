import * as common from "@grafana/grafana-foundation-sdk/common";
import { DataqueryBuilder } from "@grafana/grafana-foundation-sdk/loki";
import { PanelBuilder as LogsBuilder } from "@grafana/grafana-foundation-sdk/logs";
import { LOKI } from "../config/datasources.js";
import { LOKI_MAX_LINES, recentWarningsAndErrorsLogQL } from "../config/loki.js";

/** Recent WARNING / ERROR style lines from Loki. */
export function createLogPanel(span = 12, height = 10): LogsBuilder {
  return new LogsBuilder()
    .title("Recent Warnings and Errors")
    .description(
      "LogQL uses a generic stream selector until Alloy labels are verified on the host. See monitoring/dashboard-generator/src/config/loki.ts and README.",
    )
    .span(span)
    .height(height)
    .datasource(LOKI)
    .withTarget(
      new DataqueryBuilder()
        .datasource(LOKI)
        .expr(recentWarningsAndErrorsLogQL())
        .refId("A")
        .maxLines(LOKI_MAX_LINES),
    )
    .showTime(true)
    .showLabels(false)
    .showCommonLabels(false)
    .wrapLogMessage(true)
    .prettifyLogMessage(false)
    .enableLogDetails(true)
    .sortOrder(common.LogsSortOrder.Descending)
    .dedupStrategy(common.LogsDedupStrategy.Exact);
}
