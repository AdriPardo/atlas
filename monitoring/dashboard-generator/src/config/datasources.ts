import type { DataSourceRef } from "@grafana/grafana-foundation-sdk/dashboard";

/** Prometheus datasource UID as provisioned on Atlas Grafana. */
export const PROMETHEUS: DataSourceRef = {
  type: "prometheus",
  uid: "prometheus",
};

/** Loki datasource UID as provisioned on Atlas Grafana. */
export const LOKI: DataSourceRef = {
  type: "loki",
  uid: "loki",
};
