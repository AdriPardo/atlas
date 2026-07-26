package com.atlas.application.deployment;

import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.deployment.DeploymentStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListDeploymentsUseCase {

    private final DeploymentRepositoryPort deploymentRepository;

    @Transactional(readOnly = true)
    public PageResult<Deployment> execute(
            UUID applicationId, UUID hostId, DeploymentStatus status, PageQuery pageQuery) {
        return deploymentRepository.search(applicationId, hostId, status, pageQuery);
    }
}
