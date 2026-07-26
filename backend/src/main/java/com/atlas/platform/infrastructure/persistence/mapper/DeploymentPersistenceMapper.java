package com.atlas.platform.infrastructure.persistence.mapper;

import com.atlas.platform.domain.model.Deployment;
import com.atlas.platform.infrastructure.persistence.entity.DeploymentJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DeploymentPersistenceMapper {

    Deployment toDomain(DeploymentJpaEntity entity);

    DeploymentJpaEntity toEntity(Deployment domain);
}
