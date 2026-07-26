package com.atlas.platform.api.dto.response;

import com.atlas.platform.domain.model.DeploymentStatus;
import java.time.Instant;
import java.util.UUID;

public record DeploymentResponse(
        UUID id,
        UUID applicationId,
        UUID hostId,
        DeploymentStatus status,
        Instant startedAt,
        Instant finishedAt,
        String logs) {}
