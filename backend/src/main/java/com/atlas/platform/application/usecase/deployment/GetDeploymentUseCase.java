package com.atlas.platform.application.usecase.deployment;

import com.atlas.platform.application.security.InstallationContext;
import com.atlas.platform.domain.exception.NotFoundException;
import com.atlas.platform.domain.model.Deployment;
import com.atlas.platform.domain.port.out.DeploymentRepositoryPort;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetDeploymentUseCase {

    private final DeploymentRepositoryPort deploymentRepository;

    public GetDeploymentUseCase(DeploymentRepositoryPort deploymentRepository) {
        this.deploymentRepository = deploymentRepository;
    }

    @Transactional(readOnly = true)
    public Deployment execute(UUID id) {
        return deploymentRepository
                .findById(InstallationContext.currentInstallationId(), id)
                .orElseThrow(() -> new NotFoundException("Deployment not found: " + id));
    }
}
