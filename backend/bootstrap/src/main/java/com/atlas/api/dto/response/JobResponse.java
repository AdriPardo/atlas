package com.atlas.api.dto.response;

import com.atlas.domain.job.JobStatus;
import com.atlas.domain.job.JobType;
import java.time.Instant;
import java.util.UUID;

public record JobResponse(
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
        Instant updatedAt) {}
