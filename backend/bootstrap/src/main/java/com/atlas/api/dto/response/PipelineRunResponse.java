package com.atlas.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PipelineRunResponse(
        UUID id,
        UUID pipelineId,
        String status,
        String triggeredBy,
        UUID deploymentId,
        UUID jobId,
        Instant startedAt,
        Instant finishedAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt) {}
