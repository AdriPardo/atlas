package com.atlas.platform.infrastructure.persistence.repository;

import com.atlas.platform.infrastructure.persistence.entity.DeploymentJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DeploymentJpaRepository
        extends JpaRepository<DeploymentJpaEntity, UUID>,
                JpaSpecificationExecutor<DeploymentJpaEntity> {

    Optional<DeploymentJpaEntity> findByInstallationIdAndId(UUID installationId, UUID id);
}
