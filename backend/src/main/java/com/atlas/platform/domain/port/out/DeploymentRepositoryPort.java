package com.atlas.platform.domain.port.out;

import com.atlas.platform.domain.model.Deployment;
import com.atlas.platform.domain.model.DeploymentStatus;
import com.atlas.platform.domain.model.PageResult;
import java.util.Optional;
import java.util.UUID;

public interface DeploymentRepositoryPort {

    Optional<Deployment> findById(UUID installationId, UUID id);

    PageResult<Deployment> search(
            UUID installationId,
            UUID applicationId,
            UUID hostId,
            DeploymentStatus status,
            int page,
            int size,
            String sortBy,
            boolean ascending);
}
