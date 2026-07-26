package com.atlas.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProjectMembershipResponse(
        UUID id, UUID projectId, UUID userId, String role, Instant createdAt, Instant updatedAt) {}
