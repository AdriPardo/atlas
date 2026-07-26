package com.atlas.domain.audit;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class AuditEntry {

    private final UUID id;
    private final UUID actorUserId;
    private final String actorUsername;
    private final String action;
    private final String resourceType;
    private final UUID resourceId;
    private final String metadata;
    private final Instant createdAt;

    private AuditEntry(
            UUID id,
            UUID actorUserId,
            String actorUsername,
            String action,
            String resourceType,
            UUID resourceId,
            String metadata,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.actorUserId = actorUserId;
        this.actorUsername = actorUsername == null || actorUsername.isBlank() ? "system" : actorUsername.trim();
        this.action = Objects.requireNonNull(action);
        this.resourceType = Objects.requireNonNull(resourceType);
        this.resourceId = resourceId;
        this.metadata = metadata == null || metadata.isBlank() ? "{}" : metadata;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static AuditEntry record(
            UUID actorUserId,
            String actorUsername,
            String action,
            String resourceType,
            UUID resourceId,
            String metadata) {
        return new AuditEntry(
                UUID.randomUUID(),
                actorUserId,
                actorUsername,
                action,
                resourceType,
                resourceId,
                metadata,
                Instant.now());
    }

    public static AuditEntry rehydrate(
            UUID id,
            UUID actorUserId,
            String actorUsername,
            String action,
            String resourceType,
            UUID resourceId,
            String metadata,
            Instant createdAt) {
        return new AuditEntry(
                id, actorUserId, actorUsername, action, resourceType, resourceId, metadata, createdAt);
    }
}
