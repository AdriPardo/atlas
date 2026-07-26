package com.atlas.platform.infrastructure.persistence.mapper;

import com.atlas.platform.domain.model.UserAccount;
import com.atlas.platform.infrastructure.persistence.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserPersistenceMapper {

    @Mapping(source = "passwordHash", target = "passwordHash")
    UserAccount toDomain(UserJpaEntity entity);

    UserJpaEntity toEntity(UserAccount domain);
}
