package com.atlas.infrastructure.persistence.jpa.repository;

import com.atlas.domain.job.JobStatus;
import com.atlas.infrastructure.persistence.jpa.entity.JobJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JobJpaRepository
        extends JpaRepository<JobJpaEntity, UUID>, JpaSpecificationExecutor<JobJpaEntity> {}
