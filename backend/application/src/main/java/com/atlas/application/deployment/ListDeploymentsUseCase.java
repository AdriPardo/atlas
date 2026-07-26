package com.atlas.application.deployment;

import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
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
    private final ServiceRepositoryPort serviceRepository;

    @Transactional(readOnly = true)
    public PageResult<Deployment> execute(
            UUID serviceId,
            UUID applicationId,
            UUID hostId,
            DeploymentStatus status,
            PageQuery pageQuery) {
        UUID resolvedServiceId = serviceId;
        if (resolvedServiceId == null && applicationId != null) {
            // Deprecated filter: applicationId = project id → filter default service deployments
            resolvedServiceId = serviceRepository
                    .findDefaultByProjectId(applicationId)
                    .map(s -> s.getId())
                    .orElse(applicationId); // no match → empty via unknown service id
        }
        return deploymentRepository.search(resolvedServiceId, hostId, status, pageQuery);
    }
}
