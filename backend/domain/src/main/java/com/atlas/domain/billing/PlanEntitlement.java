package com.atlas.domain.billing;

import java.math.BigDecimal;
import java.util.Objects;
import lombok.Getter;

/**
 * Soft commercial limit for a meter on the local plan. {@code limitQuantity} of {@code -1} =
 * unlimited. Price may be zero (self-hosted community).
 */
@Getter
public class PlanEntitlement {

    private final String planCode;
    private final String meter;
    private final BigDecimal limitQuantity;
    private final String unit;
    private final int priceCents;
    private final boolean soft;

    public PlanEntitlement(
            String planCode,
            String meter,
            BigDecimal limitQuantity,
            String unit,
            int priceCents,
            boolean soft) {
        this.planCode = requireText(planCode, "planCode");
        this.meter = requireText(meter, "meter");
        this.limitQuantity = Objects.requireNonNull(limitQuantity);
        this.unit = requireText(unit, "unit");
        if (priceCents < 0) {
            throw new IllegalArgumentException("priceCents must be >= 0");
        }
        this.priceCents = priceCents;
        this.soft = soft;
    }

    public boolean isUnlimited() {
        return limitQuantity.compareTo(BigDecimal.valueOf(-1)) == 0;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
