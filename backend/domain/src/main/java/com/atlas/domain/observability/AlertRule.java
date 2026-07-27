package com.atlas.domain.observability;

import com.atlas.domain.shared.DomainException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class AlertRule {

    private final UUID id;
    private String name;
    private AlertEventType eventType;
    private UUID projectId;
    private UUID channelId;
    private AlertRuleStatus status;
    private Instant lastFiredAt;
    private String lastError;
    private final Instant createdAt;
    private Instant updatedAt;

    private AlertRule(
            UUID id,
            String name,
            AlertEventType eventType,
            UUID projectId,
            UUID channelId,
            AlertRuleStatus status,
            Instant lastFiredAt,
            String lastError,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        apply(name, eventType, projectId, channelId, status, lastFiredAt, lastError, updatedAt);
    }

    public static AlertRule create(String name, AlertEventType eventType, UUID projectId, UUID channelId) {
        Instant now = Instant.now();
        return new AlertRule(
                UUID.randomUUID(),
                name,
                eventType,
                projectId,
                channelId,
                AlertRuleStatus.OK,
                null,
                null,
                now,
                now);
    }

    public static AlertRule rehydrate(
            UUID id,
            String name,
            AlertEventType eventType,
            UUID projectId,
            UUID channelId,
            AlertRuleStatus status,
            Instant lastFiredAt,
            String lastError,
            Instant createdAt,
            Instant updatedAt) {
        return new AlertRule(
                id, name, eventType, projectId, channelId, status, lastFiredAt, lastError, createdAt, updatedAt);
    }

    public void update(String name, AlertEventType eventType, UUID projectId, UUID channelId) {
        apply(name, eventType, projectId, channelId, this.status, this.lastFiredAt, this.lastError, Instant.now());
    }

    public void silence() {
        this.status = AlertRuleStatus.SILENCED;
        this.updatedAt = Instant.now();
    }

    public void unsilence() {
        if (this.status == AlertRuleStatus.SILENCED) {
            this.status = AlertRuleStatus.OK;
            this.updatedAt = Instant.now();
        }
    }

    public void markFired() {
        Instant now = Instant.now();
        this.status = AlertRuleStatus.FIRING;
        this.lastFiredAt = now;
        this.lastError = null;
        this.updatedAt = now;
    }

    public void markDeliveryError(String message) {
        this.status = AlertRuleStatus.FIRING;
        this.lastError = requireText(message, "lastError");
        this.lastFiredAt = Instant.now();
        this.updatedAt = this.lastFiredAt;
    }

    public boolean isActive() {
        return status != AlertRuleStatus.SILENCED;
    }

    public boolean matchesProject(UUID eventProjectId) {
        if (projectId == null) {
            return true;
        }
        return eventProjectId != null && projectId.equals(eventProjectId);
    }

    private void apply(
            String name,
            AlertEventType eventType,
            UUID projectId,
            UUID channelId,
            AlertRuleStatus status,
            Instant lastFiredAt,
            String lastError,
            Instant updatedAt) {
        this.name = requireText(name, "name");
        if (this.name.length() > 128) {
            throw new DomainException("name must be <= 128 characters");
        }
        this.eventType = Objects.requireNonNull(eventType, "eventType is required");
        this.projectId = projectId;
        this.channelId = Objects.requireNonNull(channelId, "channelId is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.lastFiredAt = lastFiredAt;
        this.lastError = blankToNull(lastError);
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainException(field + " is required");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
