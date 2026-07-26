package com.atlas.platform.infrastructure.persistence.repository;

import com.atlas.platform.infrastructure.persistence.entity.HostJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HostJpaRepository
        extends JpaRepository<HostJpaEntity, UUID>, JpaSpecificationExecutor<HostJpaEntity> {

    Optional<HostJpaEntity> findByInstallationIdAndId(UUID installationId, UUID id);

    long countByInstallationId(UUID installationId);
}
