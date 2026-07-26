package com.atlas.application.port.out;

/**
 * External observability stack deep-links (ADR-0007). Empty URLs mean not configured.
 */
public interface ObservabilitySettingsPort {

    ObservabilitySettings current();

    String containerLogsDeepLink(String containerName, String hostHostname);

    String hostMetricsDeepLink(String hostHostname);

    record ObservabilitySettings(String grafanaBaseUrl, String lokiBaseUrl, boolean configured) {}
}
