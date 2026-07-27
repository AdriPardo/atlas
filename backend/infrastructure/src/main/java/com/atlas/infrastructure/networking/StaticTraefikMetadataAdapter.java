package com.atlas.infrastructure.networking;

import com.atlas.application.port.out.TraefikMetadataPort;
import com.atlas.domain.networking.Domain;
import com.atlas.infrastructure.config.AtlasProperties;
import org.springframework.stereotype.Component;

@Component
public class StaticTraefikMetadataAdapter implements TraefikMetadataPort {

    private final AtlasProperties properties;

    public StaticTraefikMetadataAdapter(AtlasProperties properties) {
        this.properties = properties;
    }

    @Override
    public TraefikRouteMetadata metadataFor(Domain domain, String routerName) {
        String certResolver = properties.getNetworking().getTraefikCertResolver();
        int backendPort = properties.getNetworking().getTraefikBackendPort();
        return TraefikRouteMetadata.of(routerName, domain.getHostname(), certResolver, backendPort);
    }
}
