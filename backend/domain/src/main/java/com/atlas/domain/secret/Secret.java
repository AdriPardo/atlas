package com.atlas.domain.secret;

import com.atlas.domain.shared.DomainException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Secret {

    private final UUID id;
    private final String name;
    private final String ciphertext;
    private final Instant createdAt;
    private Instant updatedAt;

    private Secret(UUID id, String name, String ciphertext, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.name = requireText(name, "name");
        this.ciphertext = requireText(ciphertext, "ciphertext");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    public static Secret create(String name, String ciphertext) {
        Instant now = Instant.now();
        return new Secret(UUID.randomUUID(), name, ciphertext, now, now);
    }

    public static Secret rehydrate(
            UUID id, String name, String ciphertext, Instant createdAt, Instant updatedAt) {
        return new Secret(id, name, ciphertext, createdAt, updatedAt);
    }

    public Secret withCiphertext(String newCiphertext) {
        return new Secret(id, name, newCiphertext, createdAt, Instant.now());
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainException(field + " is required");
        }
        return value.trim();
    }
}
