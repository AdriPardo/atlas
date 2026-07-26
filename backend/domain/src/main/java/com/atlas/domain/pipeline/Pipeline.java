package com.atlas.domain.pipeline;

import com.atlas.domain.shared.DomainException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Pipeline {

    private final UUID id;
    private final UUID projectId;
    private String name;
    private UUID serviceId;
    private UUID hostId;
    private final Instant createdAt;
    private Instant updatedAt;

    private Pipeline(
            UUID id,
            UUID projectId,
            String name,
            UUID serviceId,
            UUID hostId,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.projectId = Objects.requireNonNull(projectId, "projectId is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        apply(name, serviceId, hostId, updatedAt);
    }

    public static Pipeline create(UUID projectId, String name, UUID serviceId, UUID hostId) {
        Instant now = Instant.now();
        return new Pipeline(UUID.randomUUID(), projectId, name, serviceId, hostId, now, now);
    }

    public static Pipeline rehydrate(
            UUID id,
            UUID projectId,
            String name,
            UUID serviceId,
            UUID hostId,
            Instant createdAt,
            Instant updatedAt) {
        return new Pipeline(id, projectId, name, serviceId, hostId, createdAt, updatedAt);
    }

    public void update(String name, UUID serviceId, UUID hostId) {
        apply(name, serviceId, hostId, Instant.now());
    }

    private void apply(String name, UUID serviceId, UUID hostId, Instant updatedAt) {
        this.name = requireText(name, "name");
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId is required");
        this.hostId = Objects.requireNonNull(hostId, "hostId is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainException(field + " is required");
        }
        return value.trim();
    }
}
