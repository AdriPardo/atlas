package com.atlas.infrastructure.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.atlas.application.port.out.FeatureFlagPort;
import com.atlas.domain.billing.PlanCodes;
import com.atlas.domain.billing.PlanEntitlement;
import com.atlas.domain.billing.UsageMeters;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocalPlanEntitlementAdapterTest {

    @Mock
    private FeatureFlagPort featureFlagPort;

    @Test
    void communityHasFiniteProjectLimit() {
        when(featureFlagPort.currentPlanCode()).thenReturn(PlanCodes.COMMUNITY);
        LocalPlanEntitlementAdapter adapter = new LocalPlanEntitlementAdapter(featureFlagPort);
        List<PlanEntitlement> entitlements = adapter.listForCurrentPlan();
        PlanEntitlement projects = entitlements.stream()
                .filter(e -> UsageMeters.PROJECT_COUNT.equals(e.getMeter()))
                .findFirst()
                .orElseThrow();
        assertEquals(PlanCodes.COMMUNITY, adapter.currentPlanCode());
        assertFalseUnlimited(projects);
    }

    @Test
    void enterpriseUnlimitedProjects() {
        when(featureFlagPort.currentPlanCode()).thenReturn(PlanCodes.ENTERPRISE);
        LocalPlanEntitlementAdapter adapter = new LocalPlanEntitlementAdapter(featureFlagPort);
        PlanEntitlement projects = adapter.listForCurrentPlan().stream()
                .filter(e -> UsageMeters.PROJECT_COUNT.equals(e.getMeter()))
                .findFirst()
                .orElseThrow();
        assertTrue(projects.isUnlimited());
        assertEquals(PlanCodes.ENTERPRISE, projects.getPlanCode());
    }

    private static void assertFalseUnlimited(PlanEntitlement entitlement) {
        org.junit.jupiter.api.Assertions.assertFalse(entitlement.isUnlimited());
    }
}
