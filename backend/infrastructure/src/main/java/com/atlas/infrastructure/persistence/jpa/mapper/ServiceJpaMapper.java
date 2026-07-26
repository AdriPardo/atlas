package com.atlas.infrastructure.persistence.jpa.mapper;

import com.atlas.domain.service.ServiceStatus;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.infrastructure.persistence.jpa.entity.ServiceJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ServiceJpaMapper {

    public ServiceUnit toDomain(ServiceJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return ServiceUnit.rehydrate(
                entity.getId(),
                entity.getProjectId(),
                entity.getName(),
                entity.getRepositoryUrl(),
                entity.getBranch(),
                entity.getComposePath(),
                entity.getDomain(),
                entity.getEnvironment(),
                ServiceStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public ServiceJpaEntity toEntity(ServiceUnit domain) {
        if (domain == null) {
            return null;
        }
        ServiceJpaEntity entity = new ServiceJpaEntity();
        entity.setId(domain.getId());
        entity.setProjectId(domain.getProjectId());
        entity.setName(domain.getName());
        entity.setRepositoryUrl(domain.getRepositoryUrl());
        entity.setBranch(domain.getBranch());
        entity.setComposePath(domain.getComposePath());
        entity.setDomain(domain.getDomain());
        entity.setEnvironment(domain.getEnvironment());
        entity.setStatus(domain.getStatus().name());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
