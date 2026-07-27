package com.atlas.infrastructure.persistence.jpa.repository;

import com.atlas.infrastructure.persistence.jpa.entity.HostJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HostJpaRepository
        extends JpaRepository<HostJpaEntity, UUID>, JpaSpecificationExecutor<HostJpaEntity> {

    Optional<HostJpaEntity> findByHostnameIgnoreCase(String hostname);

    boolean existsByHostnameIgnoreCase(String hostname);

    boolean existsByHostnameIgnoreCaseAndIdNot(String hostname, UUID id);
}
