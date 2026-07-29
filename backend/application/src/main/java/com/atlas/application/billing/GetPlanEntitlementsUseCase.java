package com.atlas.application.billing;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.application.port.out.PlanEntitlementPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.domain.billing.PlanEntitlement;
import com.atlas.domain.billing.UsageMeters;
import com.atlas.domain.shared.ForbiddenException;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetPlanEntitlementsUseCase {

    private final PlanEntitlementPort planEntitlementPort;
    private final ProjectRepositoryPort projectRepository;
    private final HostRepositoryPort hostRepository;
    private final DeploymentRepositoryPort deploymentRepository;
    private final ProjectAuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public Result execute() {
        CurrentUserPort.Actor actor = authorizationService.requireActor();
        if (!actor.isAdmin()) {
            throw new ForbiddenException("Only ADMIN can read billing entitlements");
        }
        List<LiveGauge> gauges = List.of(
                new LiveGauge(UsageMeters.PROJECT_COUNT, BigDecimal.valueOf(projectRepository.count())),
                new LiveGauge(UsageMeters.HOST_COUNT, BigDecimal.valueOf(hostRepository.count())),
                new LiveGauge(UsageMeters.DEPLOYMENT_COUNT, BigDecimal.valueOf(deploymentRepository.count())));
        return new Result(
                planEntitlementPort.currentPlanCode(), planEntitlementPort.listForCurrentPlan(), gauges);
    }

    public record LiveGauge(String meter, BigDecimal quantity) {}

    public record Result(String planCode, List<PlanEntitlement> entitlements, List<LiveGauge> gauges) {}
}
