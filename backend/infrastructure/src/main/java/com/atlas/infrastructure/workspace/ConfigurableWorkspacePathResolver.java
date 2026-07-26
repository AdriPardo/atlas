package com.atlas.infrastructure.workspace;

import com.atlas.application.deployment.ExecuteDeployServiceJobUseCase;
import com.atlas.infrastructure.config.AtlasProperties;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ConfigurableWorkspacePathResolver implements ExecuteDeployServiceJobUseCase.WorkspacePathResolver {

    private final AtlasProperties properties;

    public ConfigurableWorkspacePathResolver(AtlasProperties properties) {
        this.properties = properties;
    }

    @Override
    public Path resolve(UUID deploymentId) {
        return Path.of(properties.getWorkspace().getDir(), deploymentId.toString());
    }
}
