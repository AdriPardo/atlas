package com.atlas.infrastructure.backup;

import com.atlas.application.backup.EnqueueBackupDatabaseUseCase;
import com.atlas.application.port.out.BackupPolicyPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "atlas.backup.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseBackupScheduler {

    private final EnqueueBackupDatabaseUseCase enqueueBackupDatabaseUseCase;
    private final BackupPolicyPort backupPolicy;

    @Scheduled(cron = "${atlas.backup.cron:0 30 2 * * *}")
    public void schedule() {
        if (!backupPolicy.enabled()) {
            return;
        }
        var job = enqueueBackupDatabaseUseCase.execute();
        log.info("Scheduled database backup enqueued: jobId={}", job.getId());
    }
}
