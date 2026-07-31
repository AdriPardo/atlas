package com.atlas.infrastructure.workspace;

import com.atlas.application.deployment.ResolvePlacementRuntimeCapabilityUseCase;
import com.atlas.infrastructure.config.AtlasProperties;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ConfigurablePlacementWorkspacePathResolver
        implements ResolvePlacementRuntimeCapabilityUseCase.PlacementWorkspacePathResolver {

    private final AtlasProperties properties;

    public ConfigurablePlacementWorkspacePathResolver(AtlasProperties properties) {
        this.properties = properties;
    }

    @Override
    public Path resolve(UUID serviceId) {
        return Path.of(properties.getWorkspace().getDir(), "placement", serviceId.toString());
    }
}
