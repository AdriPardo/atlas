package com.atlas.application.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.application.port.out.PlanEntitlementPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.UsageRecordRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.billing.PlanEntitlement;
import com.atlas.domain.billing.UsageMeters;
import com.atlas.domain.billing.UsageRecord;
import com.atlas.domain.shared.ForbiddenException;
import com.atlas.domain.user.Role;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillingUseCasesTest {

    @Mock
    private UsageRecordRepositoryPort usageRecordRepository;

    @Mock
    private PlanEntitlementPort planEntitlementPort;

    @Mock
    private ProjectRepositoryPort projectRepository;

    @Mock
    private HostRepositoryPort hostRepository;

    @Mock
    private DeploymentRepositoryPort deploymentRepository;

    @Mock
    private ProjectAuthorizationService authorizationService;

    @InjectMocks
    private ListUsageRecordsUseCase listUsageRecordsUseCase;

    @InjectMocks
    private GetPlanEntitlementsUseCase getPlanEntitlementsUseCase;

    @InjectMocks
    private RecordUsageUseCase recordUsageUseCase;

    @Test
    void listUsageRequiresAdmin() {
        when(authorizationService.requireActor())
                .thenReturn(new CurrentUserPort.Actor(UUID.randomUUID(), "op", Role.OPERATOR));
        assertThrows(
                ForbiddenException.class, () -> listUsageRecordsUseCase.execute(new PageQuery(0, 20, "createdAt,desc")));
    }

    @Test
    void listUsageReturnsPageForAdmin() {
        when(authorizationService.requireActor())
                .thenReturn(new CurrentUserPort.Actor(UUID.randomUUID(), "admin", Role.ADMIN));
        UsageRecord record = UsageRecord.record(
                UsageMeters.DEPLOY_COUNT,
                BigDecimal.ONE,
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-31T23:59:59Z"),
                "{}");
        when(usageRecordRepository.search(any()))
                .thenReturn(PageResult.of(List.of(record), 0, 20, 1, "createdAt,desc"));

        PageResult<UsageRecord> page = listUsageRecordsUseCase.execute(new PageQuery(0, 20, "createdAt,desc"));
        assertEquals(1, page.content().size());
        assertEquals(UsageMeters.DEPLOY_COUNT, page.content().getFirst().getMeter());
    }

    @Test
    void entitlementsIncludeLiveGauges() {
        when(authorizationService.requireActor())
                .thenReturn(new CurrentUserPort.Actor(UUID.randomUUID(), "admin", Role.ADMIN));
        when(planEntitlementPort.currentPlanCode()).thenReturn("community");
        when(planEntitlementPort.listForCurrentPlan())
                .thenReturn(List.of(new PlanEntitlement(
                        "community", UsageMeters.PROJECT_COUNT, BigDecimal.valueOf(100), "projects", 0, true)));
        when(projectRepository.count()).thenReturn(3L);
        when(hostRepository.count()).thenReturn(2L);
        when(deploymentRepository.count()).thenReturn(7L);

        GetPlanEntitlementsUseCase.Result result = getPlanEntitlementsUseCase.execute();
        assertEquals("community", result.planCode());
        assertEquals(1, result.entitlements().size());
        assertEquals(3, result.gauges().size());
        assertEquals(0, BigDecimal.valueOf(3).compareTo(result.gauges().getFirst().quantity()));
    }

    @Test
    void recordUsagePersistsRow() {
        when(usageRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UsageRecord saved = recordUsageUseCase.execute(UsageMeters.DEPLOY_COUNT, BigDecimal.ONE, null);
        assertEquals(UsageMeters.DEPLOY_COUNT, saved.getMeter());
        verify(usageRecordRepository).save(any(UsageRecord.class));
    }
}
