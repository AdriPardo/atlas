package com.atlas.infrastructure.billing;

import com.atlas.application.port.out.PlanEntitlementPort;
import com.atlas.domain.billing.PlanEntitlement;
import com.atlas.domain.billing.UsageMeters;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Local community plan (price 0). Soft limits only — deploy/product flows are not blocked in v0.9.
 */
@Component
public class LocalPlanEntitlementAdapter implements PlanEntitlementPort {

    public static final String PLAN_COMMUNITY = "community";

    private static final List<PlanEntitlement> COMMUNITY = List.of(
            new PlanEntitlement(
                    PLAN_COMMUNITY, UsageMeters.PROJECT_COUNT, BigDecimal.valueOf(10_000), "projects", 0, true),
            new PlanEntitlement(
                    PLAN_COMMUNITY, UsageMeters.HOST_COUNT, BigDecimal.valueOf(1_000), "hosts", 0, true),
            new PlanEntitlement(
                    PLAN_COMMUNITY, UsageMeters.DEPLOY_COUNT, BigDecimal.valueOf(-1), "deploys/month", 0, true),
            new PlanEntitlement(
                    PLAN_COMMUNITY,
                    UsageMeters.DEPLOYMENT_COUNT,
                    BigDecimal.valueOf(-1),
                    "deployments",
                    0,
                    true));

    @Override
    public String currentPlanCode() {
        return PLAN_COMMUNITY;
    }

    @Override
    public List<PlanEntitlement> listForCurrentPlan() {
        return COMMUNITY;
    }
}
