package com.atlas.infrastructure.persistence.jpa.adapter;

import com.atlas.application.port.out.ApiKeyRepositoryPort;
import com.atlas.infrastructure.persistence.jpa.entity.ApiKeyJpaEntity;
import com.atlas.infrastructure.persistence.jpa.repository.ApiKeyJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ApiKeyRepositoryAdapter implements ApiKeyRepositoryPort {

    private final ApiKeyJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findActiveUserIdByHash(String keyHash) {
        return repository.findByKeyHashAndRevokedAtIsNull(keyHash).map(ApiKeyJpaEntity::getUserId);
    }

    @Override
    @Transactional
    public void touchLastUsed(String keyHash, Instant when) {
        repository.findByKeyHashAndRevokedAtIsNull(keyHash).ifPresent(entity -> {
            entity.setLastUsedAt(when);
            repository.save(entity);
        });
    }

    @Override
    @Transactional
    public Stored insert(UUID userId, String name, String prefix, String keyHash, Instant createdAt) {
        ApiKeyJpaEntity entity = new ApiKeyJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setName(name);
        entity.setKeyPrefix(prefix);
        entity.setKeyHash(keyHash);
        entity.setCreatedAt(createdAt);
        ApiKeyJpaEntity saved = repository.saveAndFlush(entity);
        return toStored(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Stored> listByUser(UUID userId) {
        return repository.findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(userId).stream()
                .map(this::toStored)
                .toList();
    }

    @Override
    @Transactional
    public boolean revoke(UUID id, UUID userId, Instant when) {
        return repository
                .findById(id)
                .filter(entity -> entity.getUserId().equals(userId) && entity.getRevokedAt() == null)
                .map(entity -> {
                    entity.setRevokedAt(when);
                    repository.save(entity);
                    return true;
                })
                .orElse(false);
    }

    private Stored toStored(ApiKeyJpaEntity entity) {
        return new Stored(
                entity.getId(),
                entity.getUserId(),
                entity.getName(),
                entity.getKeyPrefix(),
                entity.getCreatedAt(),
                entity.getLastUsedAt(),
                entity.getRevokedAt());
    }
}
