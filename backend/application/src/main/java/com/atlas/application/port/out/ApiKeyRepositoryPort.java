package com.atlas.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepositoryPort {

    Optional<UUID> findActiveUserIdByHash(String keyHash);

    void touchLastUsed(String keyHash, Instant when);

    Stored insert(UUID userId, String name, String prefix, String keyHash, Instant createdAt);

    List<Stored> listByUser(UUID userId);

    boolean revoke(UUID id, UUID userId, Instant when);

    record Stored(
            UUID id,
            UUID userId,
            String name,
            String prefix,
            Instant createdAt,
            Instant lastUsedAt,
            Instant revokedAt) {}
}
