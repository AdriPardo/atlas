package com.atlas.platform.infrastructure.persistence.mapper;

import com.atlas.platform.domain.model.Application;
import com.atlas.platform.infrastructure.persistence.entity.ApplicationJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApplicationPersistenceMapper {

    Application toDomain(ApplicationJpaEntity entity);

    ApplicationJpaEntity toEntity(Application domain);
}
