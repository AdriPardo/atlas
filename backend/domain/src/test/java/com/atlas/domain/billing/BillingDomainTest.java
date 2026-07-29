package com.atlas.domain.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UsageRecordTest {

    @Test
    void recordsMeterForPeriod() {
        Instant start = Instant.parse("2026-07-01T00:00:00Z");
        Instant end = Instant.parse("2026-07-31T23:59:59.999Z");
        UsageRecord record = UsageRecord.record(UsageMeters.DEPLOY_COUNT, BigDecimal.ONE, start, end, "{}");

        assertEquals(UsageMeters.DEPLOY_COUNT, record.getMeter());
        assertEquals(0, BigDecimal.ONE.compareTo(record.getQuantity()));
        assertEquals("{}", record.getDimensions());
    }

    @Test
    void rejectsNegativeQuantity() {
        Instant start = Instant.parse("2026-07-01T00:00:00Z");
        Instant end = Instant.parse("2026-07-31T23:59:59.999Z");
        assertThrows(
                IllegalArgumentException.class,
                () -> UsageRecord.record(UsageMeters.DEPLOY_COUNT, BigDecimal.valueOf(-1), start, end, null));
    }
}

class PlanEntitlementTest {

    @Test
    void unlimitedWhenLimitIsMinusOne() {
        PlanEntitlement entitlement = new PlanEntitlement(
                "community", UsageMeters.DEPLOY_COUNT, BigDecimal.valueOf(-1), "deploys", 0, true);
        assertTrue(entitlement.isUnlimited());
        assertEquals(0, entitlement.getPriceCents());
        assertTrue(entitlement.isSoft());
    }

    @Test
    void finiteLimitNotUnlimited() {
        PlanEntitlement entitlement = new PlanEntitlement(
                "community", UsageMeters.PROJECT_COUNT, BigDecimal.valueOf(100), "projects", 0, true);
        assertFalse(entitlement.isUnlimited());
    }
}
