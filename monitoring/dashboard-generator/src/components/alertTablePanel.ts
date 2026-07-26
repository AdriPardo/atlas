import * as common from "@grafana/grafana-foundation-sdk/common";
import { MappingType } from "@grafana/grafana-foundation-sdk/dashboard";
import { DataqueryBuilder, PromQueryFormat } from "@grafana/grafana-foundation-sdk/prometheus";
import { PanelBuilder as TableBuilder } from "@grafana/grafana-foundation-sdk/table";
import { PROMETHEUS } from "../config/datasources.js";
import { COLORS } from "../config/thresholds.js";

/** Active firing alerts from the ALERTS metric. */
export function createAlertTablePanel(span = 12, height = 10): TableBuilder {
  return new TableBuilder()
    .title("Active Alerts")
    .description(
      "Firing alerts from Prometheus `ALERTS`. Empty result means no active alerts (not an error).",
    )
    .span(span)
    .height(height)
    .datasource(PROMETHEUS)
    .noValue("No active alerts")
    .withTarget(
      new DataqueryBuilder()
        .datasource(PROMETHEUS)
        .expr('ALERTS{alertstate="firing"}')
        .refId("A")
        .instant()
        .format(PromQueryFormat.Table)
        .legendFormat("__auto"),
    )
    .withTransformation({
      id: "organize",
      options: {
        excludeByName: {
          Time: true,
          __name__: true,
          Value: true,
          alertstate: true,
          prometheus: true,
          prometheus_replica: true,
        },
        renameByName: {
          severity: "Severity",
          alertname: "Alert",
          job: "Service",
          instance: "Instance",
        },
        indexByName: {
          Severity: 0,
          Alert: 1,
          Service: 2,
          Instance: 3,
        },
      },
    })
    .withTransformation({
      id: "sortBy",
      options: {
        fields: {},
        sort: [{ field: "Severity", desc: false }],
      },
    })
    .mappings([
      {
        type: MappingType.ValueToText,
        options: {
          critical: { text: "critical", color: COLORS.red },
          warning: { text: "warning", color: COLORS.yellow },
        },
      },
    ])
    .overrideByName("Severity", [
      {
        id: "custom.cellOptions",
        value: {
          type: common.TableCellDisplayMode.ColorBackground,
          mode: "basic",
        },
      },
    ])
    .sortBy([new common.TableSortByFieldStateBuilder().displayName("Severity").desc(false)]);
}
