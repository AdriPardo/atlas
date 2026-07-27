package com.atlas.infrastructure.persistence.jpa.repository;

import com.atlas.infrastructure.persistence.jpa.entity.DomainJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DomainJpaRepository extends JpaRepository<DomainJpaEntity, UUID> {

    List<DomainJpaEntity> findByProjectIdOrderByCreatedAtAsc(UUID projectId);

    boolean existsByProjectIdAndHostnameIgnoreCase(UUID projectId, String hostname);

    boolean existsByProjectIdAndHostnameIgnoreCaseAndIdNot(UUID projectId, String hostname, UUID id);
}
