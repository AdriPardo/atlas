package com.atlas.infrastructure.persistence.jpa.repository;

import com.atlas.infrastructure.persistence.jpa.entity.ApiKeyJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyJpaRepository extends JpaRepository<ApiKeyJpaEntity, UUID> {

    Optional<ApiKeyJpaEntity> findByKeyHashAndRevokedAtIsNull(String keyHash);

    List<ApiKeyJpaEntity> findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(UUID userId);
}
