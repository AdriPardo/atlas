package com.atlas.application.port.out;

import com.atlas.domain.billing.PlanEntitlement;
import java.util.List;

public interface PlanEntitlementPort {

    String currentPlanCode();

    List<PlanEntitlement> listForCurrentPlan();
}
