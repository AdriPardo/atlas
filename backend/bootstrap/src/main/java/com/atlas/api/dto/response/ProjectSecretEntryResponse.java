package com.atlas.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProjectSecretEntryResponse(
        String kind,
        UUID secretId,
        String name,
        String secretName,
        UUID bindingId,
        String alias,
        Instant createdAt,
        Instant updatedAt) {}
