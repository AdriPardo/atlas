package com.atlas.api.dto.response;

import com.atlas.domain.deployment.DeploymentStatus;
import java.time.Instant;
import java.util.UUID;

public record DeploymentResponse(
        UUID id,
        UUID serviceId,
        /** Deprecated alias of serviceId for transitional clients; prefer serviceId. */
        UUID applicationId,
        UUID hostId,
        DeploymentStatus status,
        Instant startedAt,
        Instant finishedAt,
        String logs,
        Instant createdAt,
        Instant updatedAt) {}
