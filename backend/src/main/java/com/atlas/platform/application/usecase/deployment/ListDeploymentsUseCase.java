package com.atlas.platform.application.usecase.deployment;

import com.atlas.platform.application.security.InstallationContext;
import com.atlas.platform.domain.model.Deployment;
import com.atlas.platform.domain.model.DeploymentStatus;
import com.atlas.platform.domain.model.PageResult;
import com.atlas.platform.domain.port.out.DeploymentRepositoryPort;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListDeploymentsUseCase {

    private final DeploymentRepositoryPort deploymentRepository;

    public ListDeploymentsUseCase(DeploymentRepositoryPort deploymentRepository) {
        this.deploymentRepository = deploymentRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<Deployment> execute(
            UUID applicationId,
            UUID hostId,
            DeploymentStatus status,
            int page,
            int size,
            String sortBy,
            boolean ascending) {
        return deploymentRepository.search(
                InstallationContext.currentInstallationId(),
                applicationId,
                hostId,
                status,
                page,
                size,
                sortBy,
                ascending);
    }
}
