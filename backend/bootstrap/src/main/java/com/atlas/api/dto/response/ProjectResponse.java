package com.atlas.api.dto.response;

import com.atlas.domain.project.ProjectStatus;
import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID organizationId,
        String name,
        String slug,
        String description,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt) {}
