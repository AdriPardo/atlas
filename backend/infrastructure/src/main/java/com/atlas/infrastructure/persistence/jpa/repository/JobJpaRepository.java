package com.atlas.infrastructure.persistence.jpa.repository;

import com.atlas.domain.job.JobStatus;
import com.atlas.infrastructure.persistence.jpa.entity.JobJpaEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobJpaRepository
        extends JpaRepository<JobJpaEntity, UUID>, JpaSpecificationExecutor<JobJpaEntity> {

    @Modifying(clearAutomatically = true)
    @Query("delete from JobJpaEntity j where j.status in :statuses and j.createdAt < :cutoff")
    int deleteByStatusInAndCreatedAtBefore(
            @Param("statuses") Collection<JobStatus> statuses, @Param("cutoff") Instant cutoff);
}
