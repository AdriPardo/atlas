package com.atlas.application.port.out;

import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.deployment.DeploymentStatus;
import java.util.Optional;
import java.util.UUID;

public interface DeploymentRepositoryPort {

    Deployment save(Deployment deployment);

    Optional<Deployment> findById(UUID id);

    PageResult<Deployment> search(
            UUID applicationId, UUID hostId, DeploymentStatus status, PageQuery pageQuery);

    void deleteById(UUID id);

    boolean existsByApplicationId(UUID applicationId);

    boolean existsByHostId(UUID hostId);

    long count();
}
