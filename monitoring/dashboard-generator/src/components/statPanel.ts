import * as common from "@grafana/grafana-foundation-sdk/common";
import {
  MappingType,
  type ThresholdsConfigBuilder,
  type ValueMapping,
} from "@grafana/grafana-foundation-sdk/dashboard";
import { DataqueryBuilder, PromQueryFormat } from "@grafana/grafana-foundation-sdk/prometheus";
import { PanelBuilder as StatBuilder } from "@grafana/grafana-foundation-sdk/stat";
import { PROMETHEUS } from "../config/datasources.js";

export interface InstantStatOptions {
  title: string;
  description: string;
  expr: string;
  span: number;
  height: number;
  unit?: string;
  min?: number;
  max?: number;
  decimals?: number;
  thresholds?: ThresholdsConfigBuilder;
  mappings?: ValueMapping[];
  colorMode?: common.BigValueColorMode;
  graphMode?: common.BigValueGraphMode;
  textMode?: common.BigValueTextMode;
  noValue?: string;
  legendFormat?: string;
}

export function instantPrometheusQuery(
  expr: string,
  refId: string,
  legendFormat = "",
): DataqueryBuilder {
  return new DataqueryBuilder()
    .datasource(PROMETHEUS)
    .expr(expr)
    .refId(refId)
    .instant()
    .format(PromQueryFormat.TimeSeries)
    .legendFormat(legendFormat);
}

export function createInstantStatPanel(opts: InstantStatOptions): StatBuilder {
  const panel = new StatBuilder()
    .title(opts.title)
    .description(opts.description)
    .span(opts.span)
    .height(opts.height)
    .datasource(PROMETHEUS)
    .withTarget(instantPrometheusQuery(opts.expr, "A", opts.legendFormat ?? ""))
    .reduceOptions(new common.ReduceDataOptionsBuilder().calcs(["lastNotNull"]))
    .colorMode(opts.colorMode ?? common.BigValueColorMode.Background)
    .graphMode(opts.graphMode ?? common.BigValueGraphMode.None)
    .textMode(opts.textMode ?? common.BigValueTextMode.Auto);

  if (opts.unit !== undefined) {
    panel.unit(opts.unit);
  }
  if (opts.min !== undefined) {
    panel.min(opts.min);
  }
  if (opts.max !== undefined) {
    panel.max(opts.max);
  }
  if (opts.decimals !== undefined) {
    panel.decimals(opts.decimals);
  }
  if (opts.thresholds !== undefined) {
    panel.thresholds(opts.thresholds);
  }
  if (opts.mappings !== undefined) {
    panel.mappings(opts.mappings);
  }
  if (opts.noValue !== undefined) {
    panel.noValue(opts.noValue);
  }

  return panel;
}

export function valueMappings(
  entries: Array<{ value: string; text: string; color: string }>,
): ValueMapping[] {
  const options: Record<string, { text: string; color: string }> = {};
  for (const entry of entries) {
    options[entry.value] = { text: entry.text, color: entry.color };
  }
  return [
    {
      type: MappingType.ValueToText,
      options,
    },
  ];
}
