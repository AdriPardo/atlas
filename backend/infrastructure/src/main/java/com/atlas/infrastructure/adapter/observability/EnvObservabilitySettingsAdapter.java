package com.atlas.infrastructure.adapter.observability;

import com.atlas.application.port.out.ObservabilitySettingsPort;
import com.atlas.infrastructure.config.AtlasProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public class EnvObservabilitySettingsAdapter implements ObservabilitySettingsPort {

    private final AtlasProperties properties;

    public EnvObservabilitySettingsAdapter(AtlasProperties properties) {
        this.properties = properties;
    }

    @Override
    public ObservabilitySettings current() {
        AtlasProperties.Observability obs = properties.getObservability();
        String grafana = blankToEmpty(obs.getGrafanaBaseUrl());
        String loki = blankToEmpty(obs.getLokiBaseUrl());
        return new ObservabilitySettings(grafana, loki, !grafana.isBlank() || !loki.isBlank());
    }

    @Override
    public String containerLogsDeepLink(String containerName, String hostHostname) {
        AtlasProperties.Observability obs = properties.getObservability();
        String grafana = blankToEmpty(obs.getGrafanaBaseUrl());
        if (grafana.isBlank()) {
            return "";
        }
        String name = containerName == null || containerName.isBlank() ? ".*" : containerName;
        String host = hostHostname == null || hostHostname.isBlank() ? ".*" : hostHostname;
        String logql = "{container=~\"" + escapeRegex(name) + "\"} or {host=~\"" + escapeRegex(host) + "\"}";
        String left = JSON_LEFT_PREFIX + urlEncode(logql) + JSON_LEFT_SUFFIX;
        return trimSlash(grafana) + "/explore?orgId=1&left=" + urlEncode(left);
    }

    @Override
    public String hostMetricsDeepLink(String hostHostname) {
        AtlasProperties.Observability obs = properties.getObservability();
        String grafana = blankToEmpty(obs.getGrafanaBaseUrl());
        if (grafana.isBlank()) {
            return "";
        }
        String base = trimSlash(grafana);
        String host = hostHostname == null ? "" : hostHostname;
        if (!host.isBlank()) {
            return base + "?var-host=" + urlEncode(host);
        }
        return base;
    }

    private static final String JSON_LEFT_PREFIX =
            "[\"now-1h\",\"now\",\"Loki\",{\"expr\":\"";
    private static final String JSON_LEFT_SUFFIX = "\"}]";

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String trimSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String escapeRegex(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
