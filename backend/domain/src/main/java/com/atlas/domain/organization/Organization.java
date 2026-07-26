package com.atlas.domain.organization;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Organization {

    public static final UUID DEFAULT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final UUID id;
    private final String name;
    private final String slug;
    private final String settingsJson;
    private final Instant createdAt;

    private Organization(UUID id, String name, String slug, String settingsJson, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.name = Objects.requireNonNull(name, "name is required");
        this.slug = Objects.requireNonNull(slug, "slug is required");
        this.settingsJson = settingsJson == null ? "{}" : settingsJson;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
    }

    public static Organization rehydrate(
            UUID id, String name, String slug, String settingsJson, Instant createdAt) {
        return new Organization(id, name, slug, settingsJson, createdAt);
    }
}
