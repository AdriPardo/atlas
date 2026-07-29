package com.atlas.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PlanEntitlementsResponse(
        String planCode, List<PlanEntitlementResponse> entitlements, List<LiveGaugeResponse> gauges) {

    public record PlanEntitlementResponse(
            String planCode,
            String meter,
            BigDecimal limitQuantity,
            String unit,
            int priceCents,
            boolean soft,
            boolean unlimited) {}

    public record LiveGaugeResponse(String meter, BigDecimal quantity) {}
}
