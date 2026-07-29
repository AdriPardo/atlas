package com.atlas.domain.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class UsageRecord {

    private final UUID id;
    private final String meter;
    private final BigDecimal quantity;
    private final Instant periodStart;
    private final Instant periodEnd;
    private final String dimensions;
    private final Instant createdAt;

    private UsageRecord(
            UUID id,
            String meter,
            BigDecimal quantity,
            Instant periodStart,
            Instant periodEnd,
            String dimensions,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.meter = requireText(meter, "meter");
        this.quantity = Objects.requireNonNull(quantity);
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("quantity must be >= 0");
        }
        this.periodStart = Objects.requireNonNull(periodStart);
        this.periodEnd = Objects.requireNonNull(periodEnd);
        if (periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("periodEnd must be >= periodStart");
        }
        this.dimensions = dimensions == null || dimensions.isBlank() ? "{}" : dimensions.trim();
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static UsageRecord record(
            String meter,
            BigDecimal quantity,
            Instant periodStart,
            Instant periodEnd,
            String dimensions) {
        return new UsageRecord(
                UUID.randomUUID(), meter, quantity, periodStart, periodEnd, dimensions, Instant.now());
    }

    public static UsageRecord rehydrate(
            UUID id,
            String meter,
            BigDecimal quantity,
            Instant periodStart,
            Instant periodEnd,
            String dimensions,
            Instant createdAt) {
        return new UsageRecord(id, meter, quantity, periodStart, periodEnd, dimensions, createdAt);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
