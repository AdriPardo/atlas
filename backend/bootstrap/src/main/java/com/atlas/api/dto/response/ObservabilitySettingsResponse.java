package com.atlas.api.dto.response;

public record ObservabilitySettingsResponse(
        String grafanaBaseUrl, String lokiBaseUrl, boolean configured, String hostMetricsUrl) {}
