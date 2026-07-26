package com.atlas.domain.deployment;

import com.atlas.domain.shared.DomainException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Deployment {

    private final UUID id;
    private final UUID applicationId;
    private final UUID hostId;
    private DeploymentStatus status;
    private Instant startedAt;
    private Instant finishedAt;
    private String logs;
    private final Instant createdAt;
    private Instant updatedAt;

    private Deployment(
            UUID id,
            UUID applicationId,
            UUID hostId,
            DeploymentStatus status,
            Instant startedAt,
            Instant finishedAt,
            String logs,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId is required");
        this.hostId = Objects.requireNonNull(hostId, "hostId is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        apply(status, startedAt, finishedAt, logs, updatedAt);
    }

    public static Deployment create(UUID applicationId, UUID hostId) {
        Instant now = Instant.now();
        return new Deployment(
                UUID.randomUUID(),
                applicationId,
                hostId,
                DeploymentStatus.PENDING,
                null,
                null,
                "",
                now,
                now);
    }

    public static Deployment rehydrate(
            UUID id,
            UUID applicationId,
            UUID hostId,
            DeploymentStatus status,
            Instant startedAt,
            Instant finishedAt,
            String logs,
            Instant createdAt,
            Instant updatedAt) {
        return new Deployment(
                id,
                applicationId,
                hostId,
                status,
                startedAt,
                finishedAt,
                logs,
                createdAt,
                updatedAt);
    }

    public void updateStatus(DeploymentStatus status, Instant startedAt, Instant finishedAt, String logs) {
        apply(status, startedAt, finishedAt, logs, Instant.now());
    }

    public void markRunning() {
        Instant now = Instant.now();
        apply(DeploymentStatus.RUNNING, startedAt == null ? now : startedAt, null, logs, now);
    }

    public void appendLog(String chunk) {
        if (chunk == null || chunk.isBlank()) {
            return;
        }
        String next = logs == null || logs.isBlank() ? chunk : logs + "\n" + chunk;
        apply(status, startedAt, finishedAt, next, Instant.now());
    }

    public void markSucceeded() {
        Instant now = Instant.now();
        apply(DeploymentStatus.SUCCEEDED, startedAt == null ? now : startedAt, now, logs, now);
    }

    public void markFailed(String reason) {
        Instant now = Instant.now();
        if (reason != null && !reason.isBlank()) {
            String next = logs == null || logs.isBlank() ? reason : logs + "\n" + reason;
            apply(DeploymentStatus.FAILED, startedAt == null ? now : startedAt, now, next, now);
            return;
        }
        apply(DeploymentStatus.FAILED, startedAt == null ? now : startedAt, now, logs, now);
    }

    private void apply(
            DeploymentStatus status,
            Instant startedAt,
            Instant finishedAt,
            String logs,
            Instant updatedAt) {
        this.status = Objects.requireNonNull(status, "status is required");
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.logs = logs == null ? "" : logs;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");

        if (finishedAt != null && startedAt != null && finishedAt.isBefore(startedAt)) {
            throw new DomainException("finishedAt cannot be before startedAt");
        }
    }
}
