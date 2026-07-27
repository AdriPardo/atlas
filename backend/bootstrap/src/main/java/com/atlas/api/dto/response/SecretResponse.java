package com.atlas.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SecretResponse(
        UUID id, UUID projectId, String name, Instant createdAt, Instant updatedAt) {}
