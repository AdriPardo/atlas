import * as common from "@grafana/grafana-foundation-sdk/common";
import type { ThresholdsConfigBuilder } from "@grafana/grafana-foundation-sdk/dashboard";
import * as units from "@grafana/grafana-foundation-sdk/units";
import { createInstantStatPanel } from "./statPanel.js";

export interface ResourceGaugeOptions {
  title: string;
  description: string;
  expr: string;
  thresholds: ThresholdsConfigBuilder;
  unit?: string;
  span?: number;
  height?: number;
}

/** Instant resource gauge (CPU / memory / disk style). */
export function createResourcePanel(opts: ResourceGaugeOptions) {
  return createInstantStatPanel({
    title: opts.title,
    description: opts.description,
    expr: opts.expr,
    span: opts.span ?? 6,
    height: opts.height ?? 6,
    unit: opts.unit ?? units.Percent,
    min: 0,
    max: 100,
    decimals: 1,
    thresholds: opts.thresholds,
    colorMode: common.BigValueColorMode.Background,
    graphMode: common.BigValueGraphMode.None,
  });
}

/** Instant network throughput (bytes/sec), current value only. */
export function createNetworkThroughputPanel(opts: {
  title: string;
  description: string;
  expr: string;
  span?: number;
  height?: number;
}) {
  return createInstantStatPanel({
    title: opts.title,
    description: opts.description,
    expr: opts.expr,
    span: opts.span ?? 6,
    height: opts.height ?? 6,
    unit: units.BytesPerSecondSI,
    decimals: 1,
    colorMode: common.BigValueColorMode.Value,
    graphMode: common.BigValueGraphMode.None,
  });
}
