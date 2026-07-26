import * as common from "@grafana/grafana-foundation-sdk/common";
import {
  MappingType,
  type DashboardLinkBuilder,
  DashboardLinkBuilder as LinkBuilder,
  DashboardLinkType,
} from "@grafana/grafana-foundation-sdk/dashboard";
import { DataqueryBuilder, PromQueryFormat } from "@grafana/grafana-foundation-sdk/prometheus";
import { PanelBuilder as TableBuilder } from "@grafana/grafana-foundation-sdk/table";
import { PROMETHEUS } from "../config/datasources.js";
import { COLORS } from "../config/thresholds.js";
import { SERVICE_DASHBOARD_UIDS } from "../config/urls.js";

/**
 * Service status table from `up{job!=""}`.
 * Links are added only for jobs with a verified dashboard UID in SERVICE_DASHBOARD_UIDS.
 */
export function createServiceStatusPanel(span = 24, height = 10): TableBuilder {
  const panel = new TableBuilder()
    .title("Service Status")
    .description(
      "Prometheus scrape target health (`up`). DOWN rows sort first. Dashboard links appear only when a matching UID exists under monitoring/dashboards/official.",
    )
    .span(span)
    .height(height)
    .datasource(PROMETHEUS)
    .withTarget(
      new DataqueryBuilder()
        .datasource(PROMETHEUS)
        .expr('up{job!=""}')
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
          Environment: true,
          cluster: true,
          container: true,
          endpoint: true,
          namespace: true,
          pod: true,
          prometheus: true,
          prometheus_replica: true,
          service: true,
          MetricsPath: true,
        },
        renameByName: {
          job: "Service",
          instance: "Instance",
          Value: "Status",
        },
        indexByName: {
          Service: 0,
          Instance: 1,
          Status: 2,
        },
      },
    })
    .withTransformation({
      id: "sortBy",
      options: {
        fields: {},
        sort: [
          { field: "Status", desc: false },
          { field: "Service", desc: false },
        ],
      },
    })
    .mappings([
      {
        type: MappingType.ValueToText,
        options: {
          "0": { text: "DOWN", color: COLORS.red },
          "1": { text: "UP", color: COLORS.green },
        },
      },
    ])
    .overrideByName("Status", [
      {
        id: "custom.cellOptions",
        value: {
          type: common.TableCellDisplayMode.ColorBackground,
          mode: "basic",
        },
      },
    ])
    .sortBy([new common.TableSortByFieldStateBuilder().displayName("Status").desc(false)]);

  const links = serviceDashboardLinks();
  if (links.length > 0) {
    panel.links(links);
  }

  return panel;
}

function serviceDashboardLinks(): DashboardLinkBuilder[] {
  return Object.entries(SERVICE_DASHBOARD_UIDS).map(([job, uid]) =>
    new LinkBuilder(`${job} dashboard`)
      .type(DashboardLinkType.Link)
      .url(`/d/${uid}`)
      .targetBlank(false)
      .keepTime(true),
  );
}
