package com.atlas.platform.infrastructure.persistence.mapper;

import com.atlas.platform.domain.model.Host;
import com.atlas.platform.infrastructure.persistence.entity.HostJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HostPersistenceMapper {

    Host toDomain(HostJpaEntity entity);

    HostJpaEntity toEntity(Host domain);
}
