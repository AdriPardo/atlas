package com.atlas.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PipelineResponse(
        UUID id,
        UUID projectId,
        String name,
        UUID serviceId,
        UUID hostId,
        Instant createdAt,
        Instant updatedAt) {}
