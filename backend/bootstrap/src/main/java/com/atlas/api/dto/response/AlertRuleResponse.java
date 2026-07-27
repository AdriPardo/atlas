package com.atlas.api.dto.response;

import com.atlas.domain.observability.AlertEventType;
import com.atlas.domain.observability.AlertRuleStatus;
import java.time.Instant;
import java.util.UUID;

public record AlertRuleResponse(
        UUID id,
        String name,
        AlertEventType eventType,
        UUID projectId,
        UUID channelId,
        AlertRuleStatus status,
        Instant lastFiredAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt) {}
