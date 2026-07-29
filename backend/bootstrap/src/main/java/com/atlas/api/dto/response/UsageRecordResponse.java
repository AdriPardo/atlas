package com.atlas.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UsageRecordResponse(
        UUID id,
        String meter,
        BigDecimal quantity,
        Instant periodStart,
        Instant periodEnd,
        String dimensions,
        Instant createdAt) {}
