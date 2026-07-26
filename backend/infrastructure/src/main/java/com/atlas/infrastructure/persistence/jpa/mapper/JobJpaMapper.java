package com.atlas.infrastructure.persistence.jpa.mapper;

import com.atlas.domain.job.Job;
import com.atlas.infrastructure.persistence.jpa.entity.JobJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class JobJpaMapper {

    public Job toDomain(JobJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Job.rehydrate(
                entity.getId(),
                entity.getType(),
                entity.getPayload(),
                entity.getStatus(),
                entity.getAttempts(),
                entity.getMaxAttempts(),
                entity.getAvailableAt(),
                entity.getLockedAt(),
                entity.getLockedBy(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getLastError(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public JobJpaEntity toEntity(Job domain) {
        if (domain == null) {
            return null;
        }
        JobJpaEntity entity = new JobJpaEntity();
        entity.setId(domain.getId());
        entity.setType(domain.getType());
        entity.setPayload(domain.getPayload());
        entity.setStatus(domain.getStatus());
        entity.setAttempts(domain.getAttempts());
        entity.setMaxAttempts(domain.getMaxAttempts());
        entity.setAvailableAt(domain.getAvailableAt());
        entity.setLockedAt(domain.getLockedAt());
        entity.setLockedBy(domain.getLockedBy());
        entity.setStartedAt(domain.getStartedAt());
        entity.setFinishedAt(domain.getFinishedAt());
        entity.setLastError(domain.getLastError());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
