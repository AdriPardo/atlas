package com.atlas.infrastructure.persistence.jpa.repository;

import com.atlas.infrastructure.persistence.jpa.entity.PipelineRunJpaEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PipelineRunJpaRepository
        extends JpaRepository<PipelineRunJpaEntity, UUID>, JpaSpecificationExecutor<PipelineRunJpaEntity> {

    @Modifying(clearAutomatically = true)
    @Query("delete from PipelineRunJpaEntity r where r.status in :statuses and r.createdAt < :cutoff")
    int deleteByStatusInAndCreatedAtBefore(
            @Param("statuses") Collection<String> statuses, @Param("cutoff") Instant cutoff);
}
