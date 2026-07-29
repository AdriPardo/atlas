package com.atlas.infrastructure.persistence.jpa.repository;

import com.atlas.infrastructure.persistence.jpa.entity.PipelineJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PipelineJpaRepository
        extends JpaRepository<PipelineJpaEntity, UUID>, JpaSpecificationExecutor<PipelineJpaEntity> {

    boolean existsByProjectIdAndNameIgnoreCase(UUID projectId, String name);

    boolean existsByProjectIdAndNameIgnoreCaseAndIdNot(UUID projectId, String name, UUID id);

    java.util.Optional<PipelineJpaEntity> findByWebhookToken(String webhookToken);

    List<PipelineJpaEntity> findByServiceIdOrderByCreatedAtAsc(UUID serviceId);
}
