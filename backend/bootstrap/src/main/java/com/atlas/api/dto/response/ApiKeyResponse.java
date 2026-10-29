package com.atlas.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ApiKeyResponse(
        UUID id, String name, String prefix, Instant createdAt, Instant lastUsedAt, Instant revokedAt) {}
