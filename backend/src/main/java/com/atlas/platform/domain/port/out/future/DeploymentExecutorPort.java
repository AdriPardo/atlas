package com.atlas.platform.domain.port.out.future;

import java.util.UUID;

/**
 * Future port for real deployments. Not used by the MVP.
 */
public interface DeploymentExecutorPort {

    void deploy(UUID deploymentId);
}
