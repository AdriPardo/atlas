package com.atlas.api.dto.response;

import com.atlas.domain.observability.NotificationChannelType;
import java.time.Instant;
import java.util.UUID;

public record NotificationChannelResponse(
        UUID id,
        String name,
        NotificationChannelType type,
        String target,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {}
