package com.atlas.infrastructure.persistence.jpa.mapper;

import com.atlas.domain.service.ServiceExposure;
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
        ServiceExposure exposure = ServiceExposure.PUBLIC;
        if (entity.getExposure() != null && !entity.getExposure().isBlank()) {
            exposure = ServiceExposure.valueOf(entity.getExposure());
        }
        return ServiceUnit.rehydrate(
                entity.getId(),
                entity.getProjectId(),
                entity.getName(),
                entity.getRepositoryUrl(),
                entity.getBranch(),
                entity.getComposePath() == null ? "" : entity.getComposePath(),
                entity.getDomain(),
                entity.getEnvironment(),
                exposure,
                ServiceStatus.valueOf(entity.getStatus()),
                entity.getMigrationEnabled(),
                entity.getMigrationStrategy(),
                entity.getMigrationCommand(),
                entity.getMigrationContainer(),
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
        String composePath = domain.getComposePath();
        entity.setComposePath(composePath == null || composePath.isBlank() ? null : composePath);
        entity.setDomain(domain.getDomain());
        entity.setEnvironment(domain.getEnvironment());
        entity.setExposure(domain.getExposure() == null ? ServiceExposure.PUBLIC.name() : domain.getExposure().name());
        entity.setStatus(domain.getStatus().name());
        entity.setMigrationEnabled(domain.getMigrationEnabled());
        entity.setMigrationStrategy(domain.getMigrationStrategy());
        entity.setMigrationCommand(domain.getMigrationCommand());
        entity.setMigrationContainer(domain.getMigrationContainer());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
