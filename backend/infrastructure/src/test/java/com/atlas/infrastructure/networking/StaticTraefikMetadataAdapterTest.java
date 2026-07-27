package com.atlas.infrastructure.networking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlas.application.port.out.TraefikMetadataPort;
import com.atlas.domain.networking.Domain;
import com.atlas.infrastructure.config.AtlasProperties;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StaticTraefikMetadataAdapterTest {

    @Test
    void buildsLabelsFromHostnameAndConfig() {
        AtlasProperties properties = new AtlasProperties();
        properties.getNetworking().setTraefikCertResolver("cf");
        properties.getNetworking().setTraefikBackendPort(8080);
        StaticTraefikMetadataAdapter adapter = new StaticTraefikMetadataAdapter(properties);

        Domain domain = Domain.create(UUID.randomUUID(), "app.atlas.local", null);
        TraefikMetadataPort.TraefikRouteMetadata metadata = adapter.metadataFor(domain, "atlas-app");

        assertEquals("Host(`app.atlas.local`)", metadata.rule());
        assertEquals("cf", metadata.certResolver());
        assertEquals("8080", metadata.labels().get("traefik.http.services.atlas-app.loadbalancer.server.port"));
        assertTrue(Boolean.parseBoolean(metadata.labels().get("traefik.enable")));
    }
}
