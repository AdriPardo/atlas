package com.atlas.infrastructure.persistence.jpa.mapper;

import com.atlas.domain.secret.Secret;
import com.atlas.infrastructure.persistence.jpa.entity.SecretJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SecretJpaMapper {

    public Secret toDomain(SecretJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Secret.rehydrate(
                entity.getId(),
                entity.getProjectId(),
                entity.getName(),
                entity.getCiphertext(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public SecretJpaEntity toEntity(Secret domain) {
        if (domain == null) {
            return null;
        }
        SecretJpaEntity entity = new SecretJpaEntity();
        entity.setId(domain.getId());
        entity.setProjectId(domain.getProjectId());
        entity.setName(domain.getName());
        entity.setCiphertext(domain.getCiphertext());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
