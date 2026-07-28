package com.atlas.domain.job;

import com.atlas.domain.shared.DomainException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Job {

    private final UUID id;
    private final JobType type;
    private final String payload;
    private JobStatus status;
    private int attempts;
    private final int maxAttempts;
    private Instant availableAt;
    private Instant lockedAt;
    private String lockedBy;
    private Instant startedAt;
    private Instant finishedAt;
    private String lastError;
    private final Instant createdAt;
    private Instant updatedAt;

    private Job(
            UUID id,
            JobType type,
            String payload,
            JobStatus status,
            int attempts,
            int maxAttempts,
            Instant availableAt,
            Instant lockedAt,
            String lockedBy,
            Instant startedAt,
            Instant finishedAt,
            String lastError,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.type = Objects.requireNonNull(type, "type is required");
        this.payload = payload == null || payload.isBlank() ? "{}" : payload;
        this.maxAttempts = maxAttempts;
        if (maxAttempts <= 0) {
            throw new DomainException("maxAttempts must be > 0");
        }
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        apply(
                status,
                attempts,
                availableAt,
                lockedAt,
                lockedBy,
                startedAt,
                finishedAt,
                lastError,
                updatedAt);
    }

    public static Job enqueue(JobType type, String payload, int maxAttempts) {
        Instant now = Instant.now();
        return new Job(
                UUID.randomUUID(),
                type,
                payload,
                JobStatus.PENDING,
                0,
                maxAttempts,
                now,
                null,
                null,
                null,
                null,
                null,
                now,
                now);
    }

    public static Job rehydrate(
            UUID id,
            JobType type,
            String payload,
            JobStatus status,
            int attempts,
            int maxAttempts,
            Instant availableAt,
            Instant lockedAt,
            String lockedBy,
            Instant startedAt,
            Instant finishedAt,
            String lastError,
            Instant createdAt,
            Instant updatedAt) {
        return new Job(
                id,
                type,
                payload,
                status,
                attempts,
                maxAttempts,
                availableAt,
                lockedAt,
                lockedBy,
                startedAt,
                finishedAt,
                lastError,
                createdAt,
                updatedAt);
    }

    public void markSucceeded() {
        Instant now = Instant.now();
        apply(JobStatus.SUCCEEDED, attempts, availableAt, null, null, startedAt, now, null, now);
    }

    public void markFailed(String error) {
        Instant now = Instant.now();
        apply(
                JobStatus.FAILED,
                attempts,
                availableAt,
                null,
                null,
                startedAt,
                now,
                error,
                now);
    }

    public void markRunning(String workerId, int newAttempts) {
        Instant now = Instant.now();
        apply(
                JobStatus.RUNNING,
                newAttempts,
                availableAt,
                now,
                workerId,
                startedAt == null ? now : startedAt,
                null,
                lastError,
                now);
    }

    /** Refreshes the lease so long-running work is not treated as stale. */
    public void touchLock() {
        if (status != JobStatus.RUNNING) {
            throw new DomainException("Only RUNNING jobs can refresh a lease");
        }
        if (lockedBy == null || lockedBy.isBlank()) {
            throw new DomainException("Cannot refresh lease without lockedBy");
        }
        Instant now = Instant.now();
        apply(status, attempts, availableAt, now, lockedBy, startedAt, finishedAt, lastError, now);
    }

    private void apply(
            JobStatus status,
            int attempts,
            Instant availableAt,
            Instant lockedAt,
            String lockedBy,
            Instant startedAt,
            Instant finishedAt,
            String lastError,
            Instant updatedAt) {
        this.status = Objects.requireNonNull(status, "status is required");
        if (attempts < 0) {
            throw new DomainException("attempts cannot be negative");
        }
        this.attempts = attempts;
        this.availableAt = Objects.requireNonNull(availableAt, "availableAt is required");
        this.lockedAt = lockedAt;
        this.lockedBy = lockedBy;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.lastError = lastError;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }
}
