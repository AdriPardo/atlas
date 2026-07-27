package com.atlas.application.cron;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.backup.EnqueueBackupDatabaseUseCase;
import com.atlas.application.host.SyncHostUseCase;
import com.atlas.application.port.out.CronJobRepositoryPort;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.domain.cron.CronJob;
import com.atlas.domain.cron.CronTargetType;
import com.atlas.domain.host.Host;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobType;
import com.atlas.domain.user.Role;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CronUseCasesTest {

    @Mock
    private CronJobRepositoryPort cronJobRepository;

    @Mock
    private HostRepositoryPort hostRepository;

    @Mock
    private ProjectAuthorizationService authorizationService;

    @Mock
    private RecordAuditUseCase recordAuditUseCase;

    @Mock
    private SyncHostUseCase syncHostUseCase;

    @Mock
    private EnqueueBackupDatabaseUseCase enqueueBackupDatabaseUseCase;

    @InjectMocks
    private ManageCronJobUseCase manageCronJobUseCase;

    @InjectMocks
    private TickCronJobsUseCase tickCronJobsUseCase;

    @Test
    void createSyncHostCronPersists() {
        UUID hostId = UUID.randomUUID();
        when(authorizationService.requireActor())
                .thenReturn(new CurrentUserPort.Actor(UUID.randomUUID(), "ops", Role.OPERATOR));
        when(hostRepository.findById(hostId))
                .thenReturn(Optional.of(Host.create("h1", "10.0.0.1", "linux", "", true, null, null, null, null)));
        when(cronJobRepository.existsByNameIgnoreCase("nightly-sync")).thenReturn(false);
        when(cronJobRepository.save(any(CronJob.class))).thenAnswer(inv -> inv.getArgument(0));

        CronJob saved = manageCronJobUseCase.create(
                "nightly-sync", "0 0 3 * * *", CronTargetType.SYNC_HOST, hostId);

        assertEquals(CronTargetType.SYNC_HOST, saved.getTargetType());
        assertEquals(hostId, saved.getTargetId());
        verify(recordAuditUseCase).execute(eq("CRON_JOB_CREATE"), eq("cron_job"), eq(saved.getId()), anyString());
    }

    @Test
    void tickFiresDueSyncHost() {
        UUID hostId = UUID.randomUUID();
        Instant created = Instant.now().minusSeconds(3600);
        CronJob cronJob = CronJob.rehydrate(
                UUID.randomUUID(),
                "sync",
                "0 * * * * *",
                CronTargetType.SYNC_HOST,
                hostId,
                true,
                created.minusSeconds(120),
                null,
                created,
                created);
        when(cronJobRepository.findEnabled()).thenReturn(List.of(cronJob));
        when(syncHostUseCase.execute(hostId)).thenReturn(Job.enqueue(JobType.SYNC_HOST, "{}", 3));
        when(cronJobRepository.save(any(CronJob.class))).thenAnswer(inv -> inv.getArgument(0));

        int fired = tickCronJobsUseCase.execute(Instant.now());

        assertEquals(1, fired);
        assertTrue(cronJob.getLastFiredAt() != null);
        verify(syncHostUseCase).execute(hostId);
    }

    @Test
    void isDueFalseWhenNextIsInFuture() {
        Instant now = Instant.parse("2026-01-01T12:00:00Z");
        CronJob cronJob = CronJob.rehydrate(
                UUID.randomUUID(),
                "future",
                "0 0 0 1 1 *",
                CronTargetType.BACKUP_DATABASE,
                null,
                true,
                now,
                null,
                now,
                now);
        assertFalse(TickCronJobsUseCase.isDue(cronJob, now));
    }
}
