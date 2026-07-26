package com.atlas.infrastructure.adapter.observability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlas.infrastructure.config.AtlasProperties;
import org.junit.jupiter.api.Test;

class EnvObservabilitySettingsAdapterTest {

    @Test
    void buildsGrafanaExploreDeepLinkWhenConfigured() {
        AtlasProperties properties = new AtlasProperties();
        properties.getObservability().setGrafanaBaseUrl("https://grafana.example/");
        EnvObservabilitySettingsAdapter adapter = new EnvObservabilitySettingsAdapter(properties);

        String link = adapter.containerLogsDeepLink("nginx", "edge-1");

        assertTrue(link.startsWith("https://grafana.example/explore?orgId=1&left="));
        assertTrue(adapter.current().configured());
    }

    @Test
    void returnsEmptyDeepLinkWhenGrafanaMissing() {
        AtlasProperties properties = new AtlasProperties();
        EnvObservabilitySettingsAdapter adapter = new EnvObservabilitySettingsAdapter(properties);

        assertTrue(adapter.containerLogsDeepLink("nginx", "edge-1").isEmpty());
        assertFalse(adapter.current().configured());
    }
}
