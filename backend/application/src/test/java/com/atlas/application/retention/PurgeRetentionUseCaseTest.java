package com.atlas.application.retention;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.JobRepositoryPort;
import com.atlas.application.port.out.PipelineRunRepositoryPort;
import com.atlas.application.port.out.RetentionPolicyPort;
import com.atlas.domain.shared.ForbiddenException;
import com.atlas.domain.user.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurgeRetentionUseCaseTest {

    @Mock
    private JobRepositoryPort jobRepository;

    @Mock
    private PipelineRunRepositoryPort pipelineRunRepository;

    @Mock
    private RetentionPolicyPort retentionPolicy;

    @Mock
    private ProjectAuthorizationService authorizationService;

    @Mock
    private RecordAuditUseCase recordAuditUseCase;

    @InjectMocks
    private PurgeRetentionUseCase useCase;

    @Test
    void skipsWhenDisabled() {
        when(retentionPolicy.enabled()).thenReturn(false);

        var result = useCase.execute();

        assertEquals(0, result.deletedJobs());
        assertEquals(false, result.ran());
        verify(jobRepository, never()).deleteTerminalOlderThan(any());
    }

    @Test
    void purgesRunsThenJobs() {
        when(retentionPolicy.enabled()).thenReturn(true);
        when(retentionPolicy.jobsRetentionDays()).thenReturn(30);
        when(retentionPolicy.pipelineRunsRetentionDays()).thenReturn(14);
        when(pipelineRunRepository.deleteTerminalOlderThan(any())).thenReturn(2);
        when(jobRepository.deleteTerminalOlderThan(any())).thenReturn(5);
        when(recordAuditUseCase.execute(anyString(), anyString(), isNull(), anyString()))
                .thenReturn(com.atlas.domain.audit.AuditEntry.record(
                        null, "system", "RETENTION_PURGE", "system", null, "{}"));

        var result = useCase.execute();

        assertTrue(result.ran());
        assertEquals(5, result.deletedJobs());
        assertEquals(2, result.deletedPipelineRuns());
    }

    @Test
    void adminPathRejectsOperator() {
        when(authorizationService.requireActor())
                .thenReturn(new CurrentUserPort.Actor(UUID.randomUUID(), "ops", Role.OPERATOR));

        assertThrows(ForbiddenException.class, () -> useCase.executeAsAdmin());
    }
}
