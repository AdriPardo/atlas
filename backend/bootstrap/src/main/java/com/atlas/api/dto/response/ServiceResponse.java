package com.atlas.api.dto.response;

import com.atlas.domain.service.ServiceExposure;
import com.atlas.domain.service.ServiceStatus;
import java.time.Instant;
import java.util.UUID;

public record ServiceResponse(
        UUID id,
        UUID projectId,
        String name,
        String repositoryUrl,
        String branch,
        String composePath,
        String domain,
        String environment,
        ServiceExposure exposure,
        ServiceStatus status,
        Boolean migrationEnabled,
        String migrationStrategy,
        String migrationCommand,
        String migrationContainer,
        Instant createdAt,
        Instant updatedAt) {}
