package com.atlas.infrastructure.persistence.jpa.mapper;

import com.atlas.domain.user.Role;
import com.atlas.domain.user.User;
import com.atlas.infrastructure.persistence.jpa.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class UserJpaMapper {

    public User toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return User.rehydrate(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                Role.valueOf(entity.getRole()));
    }
}
