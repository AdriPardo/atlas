package com.atlas.infrastructure.persistence.jpa.repository;

import com.atlas.infrastructure.persistence.jpa.entity.DeploymentJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DeploymentJpaRepository
        extends JpaRepository<DeploymentJpaEntity, UUID>, JpaSpecificationExecutor<DeploymentJpaEntity> {

    boolean existsByApplicationId(UUID applicationId);

    boolean existsByHostId(UUID hostId);
}
