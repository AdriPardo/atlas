package com.atlas.api.dto.response;

import com.atlas.domain.deployment.DeploymentStatus;
import java.time.Instant;
import java.util.UUID;

public record DeploymentResponse(
        UUID id,
        UUID applicationId,
        UUID hostId,
        DeploymentStatus status,
        Instant startedAt,
        Instant finishedAt,
        String logs,
        Instant createdAt,
        Instant updatedAt) {}
