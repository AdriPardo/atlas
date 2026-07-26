package com.atlas.infrastructure.persistence.jpa.repository;

import com.atlas.infrastructure.persistence.jpa.entity.PipelineRunJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PipelineRunJpaRepository
        extends JpaRepository<PipelineRunJpaEntity, UUID>, JpaSpecificationExecutor<PipelineRunJpaEntity> {}
