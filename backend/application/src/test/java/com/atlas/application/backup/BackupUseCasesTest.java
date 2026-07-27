package com.atlas.application.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.job.EnqueueJobUseCase;
import com.atlas.application.port.out.BackupPolicyPort;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.DatabaseBackupPort;
import com.atlas.domain.audit.AuditEntry;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobType;
import com.atlas.domain.shared.DomainException;
import com.atlas.domain.shared.ForbiddenException;
import com.atlas.domain.user.Role;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupUseCasesTest {

    @Mock
    private EnqueueJobUseCase enqueueJobUseCase;

    @Mock
    private BackupPolicyPort backupPolicy;

    @Mock
    private ProjectAuthorizationService authorizationService;

    @Mock
    private RecordAuditUseCase recordAuditUseCase;

    @Mock
    private DatabaseBackupPort databaseBackupPort;

    @InjectMocks
    private EnqueueBackupDatabaseUseCase enqueueUseCase;

    @Test
    void enqueueRejectsWhenDisabled() {
        when(backupPolicy.enabled()).thenReturn(false);
        assertThrows(DomainException.class, () -> enqueueUseCase.execute());
        verify(enqueueJobUseCase, never()).execute(any());
    }

    @Test
    void adminPathRejectsOperator() {
        when(authorizationService.requireActor())
                .thenReturn(new CurrentUserPort.Actor(UUID.randomUUID(), "ops", Role.OPERATOR));
        assertThrows(ForbiddenException.class, () -> enqueueUseCase.executeAsAdmin());
    }

    @Test
    void enqueueCreatesBackupJob() {
        when(backupPolicy.enabled()).thenReturn(true);
        when(backupPolicy.directory()).thenReturn("/var/lib/atlas/backups");
        when(backupPolicy.keepCount()).thenReturn(7);
        Job job = Job.enqueue(JobType.BACKUP_DATABASE, "{}", 2);
        when(enqueueJobUseCase.execute(any())).thenReturn(job);
        when(recordAuditUseCase.execute(anyString(), anyString(), any(), anyString()))
                .thenReturn(AuditEntry.record(null, "system", "BACKUP_DATABASE", "job", job.getId(), "{}"));

        Job result = enqueueUseCase.execute();

        assertEquals(job.getId(), result.getId());
        ArgumentCaptor<EnqueueJobUseCase.EnqueueJobCommand> captor =
                ArgumentCaptor.forClass(EnqueueJobUseCase.EnqueueJobCommand.class);
        verify(enqueueJobUseCase).execute(captor.capture());
        assertEquals(JobType.BACKUP_DATABASE, captor.getValue().type());
        assertTrue(captor.getValue().payload().contains("\"keepCount\":7"));
    }

    @Test
    void executeCreatesDumpAndPrunes(@TempDir Path tempDir) throws Exception {
        when(backupPolicy.directory()).thenReturn(tempDir.toString());
        when(backupPolicy.keepCount()).thenReturn(2);

        Path older = tempDir.resolve("atlas-20251201-000000.sql.gz");
        Path mid = tempDir.resolve("atlas-20260101-000001.sql.gz");
        Path newer = tempDir.resolve("atlas-20260102-000002.sql.gz");
        Files.writeString(older, "a");
        Files.writeString(mid, "b");
        Files.writeString(newer, "c");

        Path created = tempDir.resolve("atlas-20260727-120000.sql.gz");
        when(databaseBackupPort.dumpTo(any())).thenAnswer(inv -> {
            Files.writeString(created, "fresh");
            return created;
        });

        ExecuteBackupDatabaseJobUseCase executeUseCase =
                new ExecuteBackupDatabaseJobUseCase(databaseBackupPort, backupPolicy);
        Path result = executeUseCase.execute();

        assertEquals(created, result);
        assertTrue(Files.exists(created));
        assertTrue(Files.exists(newer));
        assertFalse(Files.exists(mid));
        assertFalse(Files.exists(older));
    }
}
