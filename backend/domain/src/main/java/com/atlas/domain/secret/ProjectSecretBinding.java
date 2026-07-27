package com.atlas.domain.secret;

import com.atlas.domain.shared.DomainException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Links an organization/global secret into a project under an alias used at resolve time
 * (e.g. {@code git.token}).
 */
@Getter
public class ProjectSecretBinding {

    private final UUID id;
    private final UUID projectId;
    private final UUID secretId;
    private final String alias;
    private final Instant createdAt;

    private ProjectSecretBinding(UUID id, UUID projectId, UUID secretId, String alias, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.projectId = Objects.requireNonNull(projectId, "projectId is required");
        this.secretId = Objects.requireNonNull(secretId, "secretId is required");
        this.alias = requireAlias(alias);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
    }

    public static ProjectSecretBinding create(UUID projectId, UUID secretId, String alias) {
        return new ProjectSecretBinding(UUID.randomUUID(), projectId, secretId, alias, Instant.now());
    }

    public static ProjectSecretBinding rehydrate(
            UUID id, UUID projectId, UUID secretId, String alias, Instant createdAt) {
        return new ProjectSecretBinding(id, projectId, secretId, alias, createdAt);
    }

    private static String requireAlias(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException("alias is required");
        }
        return value.trim();
    }
}
