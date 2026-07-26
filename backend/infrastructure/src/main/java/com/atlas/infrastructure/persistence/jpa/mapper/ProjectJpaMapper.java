package com.atlas.infrastructure.persistence.jpa.mapper;

import com.atlas.domain.project.Project;
import com.atlas.domain.project.ProjectStatus;
import com.atlas.infrastructure.persistence.jpa.entity.ProjectJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectJpaMapper {

    public Project toDomain(ProjectJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Project.rehydrate(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                ProjectStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public ProjectJpaEntity toEntity(Project domain) {
        if (domain == null) {
            return null;
        }
        ProjectJpaEntity entity = new ProjectJpaEntity();
        entity.setId(domain.getId());
        entity.setOrganizationId(domain.getOrganizationId());
        entity.setName(domain.getName());
        entity.setSlug(domain.getSlug());
        entity.setDescription(domain.getDescription());
        entity.setStatus(domain.getStatus().name());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
