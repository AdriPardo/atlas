package com.atlas.infrastructure.persistence.jpa.mapper;

import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.deployment.DeploymentStatus;
import com.atlas.infrastructure.persistence.jpa.entity.DeploymentJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class DeploymentJpaMapper {

    public Deployment toDomain(DeploymentJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Deployment.rehydrate(
                entity.getId(),
                entity.getApplicationId(),
                entity.getHostId(),
                DeploymentStatus.valueOf(entity.getStatus()),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getLogs(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public DeploymentJpaEntity toEntity(Deployment domain) {
        if (domain == null) {
            return null;
        }
        DeploymentJpaEntity entity = new DeploymentJpaEntity();
        entity.setId(domain.getId());
        entity.setApplicationId(domain.getApplicationId());
        entity.setHostId(domain.getHostId());
        entity.setStatus(domain.getStatus().name());
        entity.setStartedAt(domain.getStartedAt());
        entity.setFinishedAt(domain.getFinishedAt());
        entity.setLogs(domain.getLogs());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
