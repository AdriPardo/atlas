package com.atlas.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AuditEntryResponse(
        UUID id,
        UUID actorUserId,
        String actorUsername,
        String action,
        String resourceType,
        UUID resourceId,
        String metadata,
        Instant createdAt) {}
