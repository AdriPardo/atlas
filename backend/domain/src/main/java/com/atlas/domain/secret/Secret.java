package com.atlas.domain.secret;

import com.atlas.domain.shared.DomainException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Secret {

    private final UUID id;
    /** Null = organization/global secret; set = project-owned. */
    private final UUID projectId;
    private final String name;
    private final String ciphertext;
    private final Instant createdAt;
    private Instant updatedAt;

    private Secret(
            UUID id, UUID projectId, String name, String ciphertext, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.projectId = projectId;
        this.name = requireText(name, "name");
        this.ciphertext = requireText(ciphertext, "ciphertext");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    public static Secret createGlobal(String name, String ciphertext) {
        Instant now = Instant.now();
        return new Secret(UUID.randomUUID(), null, name, ciphertext, now, now);
    }

    /** @deprecated use {@link #createGlobal(String, String)} */
    @Deprecated
    public static Secret create(String name, String ciphertext) {
        return createGlobal(name, ciphertext);
    }

    public static Secret createForProject(UUID projectId, String name, String ciphertext) {
        Instant now = Instant.now();
        return new Secret(
                UUID.randomUUID(),
                Objects.requireNonNull(projectId, "projectId is required"),
                name,
                ciphertext,
                now,
                now);
    }

    public static Secret rehydrate(
            UUID id, UUID projectId, String name, String ciphertext, Instant createdAt, Instant updatedAt) {
        return new Secret(id, projectId, name, ciphertext, createdAt, updatedAt);
    }

    /** Backward-compatible rehydrate for global secrets. */
    public static Secret rehydrate(
            UUID id, String name, String ciphertext, Instant createdAt, Instant updatedAt) {
        return rehydrate(id, null, name, ciphertext, createdAt, updatedAt);
    }

    public boolean isGlobal() {
        return projectId == null;
    }

    public Secret withCiphertext(String newCiphertext) {
        return new Secret(id, projectId, name, newCiphertext, createdAt, Instant.now());
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainException(field + " is required");
        }
        return value.trim();
    }
}
