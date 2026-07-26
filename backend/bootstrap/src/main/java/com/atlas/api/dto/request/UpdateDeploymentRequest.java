package com.atlas.api.dto.request;

import com.atlas.domain.deployment.DeploymentStatus;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record UpdateDeploymentRequest(
        @NotNull DeploymentStatus status, Instant startedAt, Instant finishedAt, String logs) {}
