package com.atlas.infrastructure.billing;

import com.atlas.application.port.out.FeatureFlagPort;
import com.atlas.application.port.out.PlanEntitlementPort;
import com.atlas.domain.billing.PlanCodes;
import com.atlas.domain.billing.PlanEntitlement;
import com.atlas.domain.billing.UsageMeters;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Local plan entitlements (price 0). Soft limits only — deploy/product flows are not blocked in
 * v0.9. Enterprise raises gauges to unlimited.
 */
@Component
public class LocalPlanEntitlementAdapter implements PlanEntitlementPort {

    private final FeatureFlagPort featureFlagPort;

    private static final List<PlanEntitlement> COMMUNITY = List.of(
            new PlanEntitlement(
                    PlanCodes.COMMUNITY, UsageMeters.PROJECT_COUNT, BigDecimal.valueOf(10_000), "projects", 0, true),
            new PlanEntitlement(
                    PlanCodes.COMMUNITY, UsageMeters.HOST_COUNT, BigDecimal.valueOf(1_000), "hosts", 0, true),
            new PlanEntitlement(
                    PlanCodes.COMMUNITY, UsageMeters.DEPLOY_COUNT, BigDecimal.valueOf(-1), "deploys/month", 0, true),
            new PlanEntitlement(
                    PlanCodes.COMMUNITY, UsageMeters.JOB_MINUTES, BigDecimal.valueOf(-1), "job-minutes/month", 0, true),
            new PlanEntitlement(
                    PlanCodes.COMMUNITY, UsageMeters.BACKUP_GB, BigDecimal.valueOf(-1), "backup-GB/month", 0, true),
            new PlanEntitlement(
                    PlanCodes.COMMUNITY,
                    UsageMeters.DEPLOYMENT_COUNT,
                    BigDecimal.valueOf(-1),
                    "deployments",
                    0,
                    true));

    private static final List<PlanEntitlement> ENTERPRISE = List.of(
            new PlanEntitlement(
                    PlanCodes.ENTERPRISE, UsageMeters.PROJECT_COUNT, BigDecimal.valueOf(-1), "projects", 0, true),
            new PlanEntitlement(
                    PlanCodes.ENTERPRISE, UsageMeters.HOST_COUNT, BigDecimal.valueOf(-1), "hosts", 0, true),
            new PlanEntitlement(
                    PlanCodes.ENTERPRISE, UsageMeters.DEPLOY_COUNT, BigDecimal.valueOf(-1), "deploys/month", 0, true),
            new PlanEntitlement(
                    PlanCodes.ENTERPRISE, UsageMeters.JOB_MINUTES, BigDecimal.valueOf(-1), "job-minutes/month", 0, true),
            new PlanEntitlement(
                    PlanCodes.ENTERPRISE, UsageMeters.BACKUP_GB, BigDecimal.valueOf(-1), "backup-GB/month", 0, true),
            new PlanEntitlement(
                    PlanCodes.ENTERPRISE,
                    UsageMeters.DEPLOYMENT_COUNT,
                    BigDecimal.valueOf(-1),
                    "deployments",
                    0,
                    true));

    public LocalPlanEntitlementAdapter(FeatureFlagPort featureFlagPort) {
        this.featureFlagPort = featureFlagPort;
    }

    @Override
    public String currentPlanCode() {
        return featureFlagPort.currentPlanCode();
    }

    @Override
    public List<PlanEntitlement> listForCurrentPlan() {
        return PlanCodes.isEnterprise(currentPlanCode()) ? ENTERPRISE : COMMUNITY;
    }
}
