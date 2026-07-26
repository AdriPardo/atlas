package com.atlas.application.deployment;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @deprecated Use {@link DeployServiceUseCase}. Kept as thin alias for deprecated /applications deploy.
 */
@Deprecated
@Service
@RequiredArgsConstructor
public class DeployApplicationUseCase {

    private final DeployServiceUseCase deployServiceUseCase;

    public DeployServiceUseCase.DeployResult execute(UUID applicationId, UUID hostId) {
        return deployServiceUseCase.executeForProject(applicationId, hostId);
    }
}
