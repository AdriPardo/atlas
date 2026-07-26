package com.atlas.api.dto.response;

import com.atlas.domain.application.ApplicationStatus;
import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        String name,
        String description,
        String repositoryUrl,
        String branch,
        String composePath,
        String domain,
        ApplicationStatus status,
        Instant createdAt,
        Instant updatedAt) {}
