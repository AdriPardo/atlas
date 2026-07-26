package com.atlas.infrastructure.persistence.jpa.mapper;

import com.atlas.domain.pipeline.Pipeline;
import com.atlas.infrastructure.persistence.jpa.entity.PipelineJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PipelineJpaMapper {

    public Pipeline toDomain(PipelineJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Pipeline.rehydrate(
                entity.getId(),
                entity.getProjectId(),
                entity.getName(),
                entity.getServiceId(),
                entity.getHostId(),
                entity.getWebhookToken(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public PipelineJpaEntity toEntity(Pipeline domain) {
        if (domain == null) {
            return null;
        }
        PipelineJpaEntity entity = new PipelineJpaEntity();
        entity.setId(domain.getId());
        entity.setProjectId(domain.getProjectId());
        entity.setName(domain.getName());
        entity.setServiceId(domain.getServiceId());
        entity.setHostId(domain.getHostId());
        entity.setWebhookToken(domain.getWebhookToken());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
