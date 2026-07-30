package com.atlas.application.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.access.FeatureGateService;
import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.AuditRepositoryPort;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.audit.AuditEntry;
import com.atlas.domain.billing.FeatureFlags;
import com.atlas.domain.shared.ForbiddenException;
import com.atlas.domain.user.Role;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportAuditEntriesUseCaseTest {

    @Mock
    private AuditRepositoryPort auditRepository;

    @Mock
    private ProjectAuthorizationService authorizationService;

    @Mock
    private FeatureGateService featureGate;

    @InjectMocks
    private ExportAuditEntriesUseCase useCase;

    @Test
    void requiresAdmin() {
        when(authorizationService.requireActor())
                .thenReturn(new CurrentUserPort.Actor(UUID.randomUUID(), "op", Role.OPERATOR));
        assertThrows(ForbiddenException.class, useCase::execute);
    }

    @Test
    void requiresAuditExportFlag() {
        when(authorizationService.requireActor())
                .thenReturn(new CurrentUserPort.Actor(UUID.randomUUID(), "admin", Role.ADMIN));
        doThrow(new ForbiddenException("Feature 'audit_export' is not enabled for plan community"))
                .when(featureGate)
                .require(FeatureFlags.AUDIT_EXPORT);
        assertThrows(ForbiddenException.class, useCase::execute);
    }

    @Test
    void exportsEntriesWhenFlagOn() {
        when(authorizationService.requireActor())
                .thenReturn(new CurrentUserPort.Actor(UUID.randomUUID(), "admin", Role.ADMIN));
        AuditEntry entry = AuditEntry.record(
                UUID.randomUUID(), "admin", "DEPLOY", "deployment", UUID.randomUUID(), "{}");
        when(auditRepository.search(any()))
                .thenReturn(PageResult.of(List.of(entry), 0, 100, 1, "createdAt,desc"));

        List<AuditEntry> result = useCase.execute();
        assertEquals(1, result.size());
        assertEquals("DEPLOY", result.getFirst().getAction());
        verify(featureGate).require(FeatureFlags.AUDIT_EXPORT);
    }
}
