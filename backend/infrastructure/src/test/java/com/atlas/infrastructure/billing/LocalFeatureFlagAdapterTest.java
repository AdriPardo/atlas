package com.atlas.infrastructure.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlas.domain.billing.FeatureFlags;
import com.atlas.domain.billing.PlanCodes;
import com.atlas.infrastructure.config.AtlasProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LocalFeatureFlagAdapterTest {

    private AtlasProperties properties;
    private LocalFeatureFlagAdapter adapter;

    @BeforeEach
    void setUp() {
        properties = new AtlasProperties();
        adapter = new LocalFeatureFlagAdapter(properties);
    }

    @Test
    void communityDefaultsDisableAuditExport() {
        assertEquals(PlanCodes.COMMUNITY, adapter.currentPlanCode());
        assertFalse(adapter.isEnabled(FeatureFlags.ENTERPRISE));
        assertTrue(adapter.isEnabled(FeatureFlags.BILLING));
        assertFalse(adapter.isEnabled(FeatureFlags.AUDIT_EXPORT));
    }

    @Test
    void enterpriseEnablesAuditExport() {
        properties.getPlan().setCode("enterprise");
        assertEquals(PlanCodes.ENTERPRISE, adapter.currentPlanCode());
        assertTrue(adapter.isEnabled(FeatureFlags.ENTERPRISE));
        assertTrue(adapter.isEnabled(FeatureFlags.AUDIT_EXPORT));
    }

    @Test
    void auditExportOverrideWinsOverCommunity() {
        properties.getPlan().setCode("community");
        properties.getFeatures().setAuditExport("true");
        assertTrue(adapter.isEnabled(FeatureFlags.AUDIT_EXPORT));
        assertFalse(adapter.isEnabled(FeatureFlags.ENTERPRISE));
    }

    @Test
    void billingCanBeDisabled() {
        properties.getFeatures().setBilling(false);
        assertFalse(adapter.isEnabled(FeatureFlags.BILLING));
    }
}
