package com.atlas.api.dto.response;

import com.atlas.domain.cron.CronTargetType;
import java.time.Instant;
import java.util.UUID;

public record CronJobResponse(
        UUID id,
        String name,
        String cronExpression,
        CronTargetType targetType,
        UUID targetId,
        boolean enabled,
        Instant lastFiredAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt) {}
