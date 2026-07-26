package com.atlas.infrastructure.persistence.jpa.repository;

import com.atlas.infrastructure.persistence.jpa.entity.ServiceJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ServiceJpaRepository
        extends JpaRepository<ServiceJpaEntity, UUID>, JpaSpecificationExecutor<ServiceJpaEntity> {

    List<ServiceJpaEntity> findByProjectIdOrderByCreatedAtAsc(UUID projectId);

    Optional<ServiceJpaEntity> findFirstByProjectIdAndNameIgnoreCase(UUID projectId, String name);

    Optional<ServiceJpaEntity> findFirstByProjectIdOrderByCreatedAtAsc(UUID projectId);

    boolean existsByProjectIdAndNameIgnoreCase(UUID projectId, String name);

    boolean existsByProjectIdAndNameIgnoreCaseAndIdNot(UUID projectId, String name, UUID id);

    boolean existsByProjectId(UUID projectId);
}
