package com.atlas.infrastructure.persistence.jpa.mapper;

import com.atlas.domain.application.Application;
import com.atlas.domain.application.ApplicationStatus;
import com.atlas.infrastructure.persistence.jpa.entity.ApplicationJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ApplicationJpaMapper {

    public Application toDomain(ApplicationJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Application.rehydrate(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getRepositoryUrl(),
                entity.getBranch(),
                entity.getComposePath(),
                entity.getDomain(),
                ApplicationStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public ApplicationJpaEntity toEntity(Application domain) {
        if (domain == null) {
            return null;
        }
        ApplicationJpaEntity entity = new ApplicationJpaEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setRepositoryUrl(domain.getRepositoryUrl());
        entity.setBranch(domain.getBranch());
        entity.setComposePath(domain.getComposePath());
        entity.setDomain(domain.getDomain());
        entity.setStatus(domain.getStatus().name());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
