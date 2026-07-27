package com.atlas.domain.cron;

import com.atlas.domain.shared.DomainException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class CronJob {

    private final UUID id;
    private String name;
    private String cronExpression;
    private CronTargetType targetType;
    private UUID targetId;
    private boolean enabled;
    private Instant lastFiredAt;
    private String lastError;
    private final Instant createdAt;
    private Instant updatedAt;

    private CronJob(
            UUID id,
            String name,
            String cronExpression,
            CronTargetType targetType,
            UUID targetId,
            boolean enabled,
            Instant lastFiredAt,
            String lastError,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        apply(name, cronExpression, targetType, targetId, enabled, updatedAt);
        this.lastFiredAt = lastFiredAt;
        this.lastError = lastError;
    }

    public static CronJob create(
            String name, String cronExpression, CronTargetType targetType, UUID targetId) {
        Instant now = Instant.now();
        return new CronJob(
                UUID.randomUUID(), name, cronExpression, targetType, targetId, true, null, null, now, now);
    }

    public static CronJob rehydrate(
            UUID id,
            String name,
            String cronExpression,
            CronTargetType targetType,
            UUID targetId,
            boolean enabled,
            Instant lastFiredAt,
            String lastError,
            Instant createdAt,
            Instant updatedAt) {
        return new CronJob(
                id,
                name,
                cronExpression,
                targetType,
                targetId,
                enabled,
                lastFiredAt,
                lastError,
                createdAt,
                updatedAt);
    }

    public void update(
            String name,
            String cronExpression,
            CronTargetType targetType,
            UUID targetId,
            Boolean enabled) {
        apply(
                name,
                cronExpression,
                targetType,
                targetId,
                enabled == null ? this.enabled : enabled,
                Instant.now());
    }

    public void markFired() {
        this.lastFiredAt = Instant.now();
        this.lastError = null;
        this.updatedAt = this.lastFiredAt;
    }

    public void markError(String error) {
        this.lastError = truncate(error == null ? "unknown error" : error, 512);
        this.updatedAt = Instant.now();
    }

    private void apply(
            String name,
            String cronExpression,
            CronTargetType targetType,
            UUID targetId,
            boolean enabled,
            Instant updatedAt) {
        this.name = requireText(name, "name");
        if (this.name.length() > 128) {
            throw new DomainException("name must be <= 128 characters");
        }
        this.cronExpression = requireText(cronExpression, "cronExpression");
        if (this.cronExpression.length() > 128) {
            throw new DomainException("cronExpression must be <= 128 characters");
        }
        this.targetType = Objects.requireNonNull(targetType, "targetType is required");
        if (this.targetType == CronTargetType.SYNC_HOST && targetId == null) {
            throw new DomainException("SYNC_HOST cron requires targetId (hostId)");
        }
        if (this.targetType == CronTargetType.BACKUP_DATABASE && targetId != null) {
            throw new DomainException("BACKUP_DATABASE cron must not set targetId");
        }
        this.targetId = targetId;
        this.enabled = enabled;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainException(field + " is required");
        }
        return value.trim();
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
