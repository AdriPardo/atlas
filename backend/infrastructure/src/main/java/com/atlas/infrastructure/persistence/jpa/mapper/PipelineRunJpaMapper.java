package com.atlas.infrastructure.persistence.jpa.mapper;

import com.atlas.domain.pipeline.PipelineRun;
import com.atlas.domain.pipeline.PipelineRunStatus;
import com.atlas.infrastructure.persistence.jpa.entity.PipelineRunJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PipelineRunJpaMapper {

    public PipelineRun toDomain(PipelineRunJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return PipelineRun.rehydrate(
                entity.getId(),
                entity.getPipelineId(),
                PipelineRunStatus.valueOf(entity.getStatus()),
                entity.getTriggeredBy(),
                entity.getDeploymentId(),
                entity.getJobId(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getLastError(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public PipelineRunJpaEntity toEntity(PipelineRun domain) {
        if (domain == null) {
            return null;
        }
        PipelineRunJpaEntity entity = new PipelineRunJpaEntity();
        entity.setId(domain.getId());
        entity.setPipelineId(domain.getPipelineId());
        entity.setStatus(domain.getStatus().name());
        entity.setTriggeredBy(domain.getTriggeredBy());
        entity.setDeploymentId(domain.getDeploymentId());
        entity.setJobId(domain.getJobId());
        entity.setStartedAt(domain.getStartedAt());
        entity.setFinishedAt(domain.getFinishedAt());
        entity.setLastError(domain.getLastError());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
