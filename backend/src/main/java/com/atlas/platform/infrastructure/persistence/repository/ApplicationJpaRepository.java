package com.atlas.platform.infrastructure.persistence.repository;

import com.atlas.platform.infrastructure.persistence.entity.ApplicationJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ApplicationJpaRepository
        extends JpaRepository<ApplicationJpaEntity, UUID>,
                JpaSpecificationExecutor<ApplicationJpaEntity> {

    Optional<ApplicationJpaEntity> findByInstallationIdAndId(UUID installationId, UUID id);

    boolean existsByInstallationIdAndNameIgnoreCaseAndIdNot(
            UUID installationId, String name, UUID id);

    boolean existsByInstallationIdAndNameIgnoreCase(UUID installationId, String name);
}
