/**
 * Loki / Alloy log selector configuration.
 *
 * Alloy config and existing Loki dashboards were NOT FOUND in this repository,
 * so the stream selector cannot be verified against real labels.
 *
 * Replace `LOKI_STREAM_SELECTOR` after inspecting Alloy on the host, e.g.:
 *   - `{container=~".+"}`
 *   - `{service_name=~".+"}`
 *   - `{job="integrations/docker"}`
 *
 * Until then the panel uses a generic `job` matcher so Grafana does not get an
 * invented label name. Validate manually with the LogQL in README.
 */
export const LOKI_STREAM_SELECTOR = '{job=~".+"}';

/** Case-insensitive ERROR / WARN / FATAL / PANIC line filter. */
export const LOKI_SEVERITY_FILTER = "(?i)error|warn|warning|fatal|panic";

export function recentWarningsAndErrorsLogQL(): string {
  return `${LOKI_STREAM_SELECTOR} |~ "${LOKI_SEVERITY_FILTER}"`;
}

export const LOKI_MAX_LINES = 100;
