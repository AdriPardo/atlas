package com.atlas.application.cron;

import com.atlas.application.backup.EnqueueBackupDatabaseUseCase;
import com.atlas.application.host.SyncHostUseCase;
import com.atlas.application.port.out.CronJobRepositoryPort;
import com.atlas.domain.cron.CronJob;
import com.atlas.domain.cron.CronTargetType;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TickCronJobsUseCase {

    private final CronJobRepositoryPort cronJobRepository;
    private final SyncHostUseCase syncHostUseCase;
    private final EnqueueBackupDatabaseUseCase enqueueBackupDatabaseUseCase;

    @Transactional
    public int execute(Instant now) {
        List<CronJob> enabled = cronJobRepository.findEnabled();
        int fired = 0;
        for (CronJob cronJob : enabled) {
            try {
                if (isDue(cronJob, now)) {
                    fire(cronJob);
                    cronJob.markFired();
                    cronJobRepository.save(cronJob);
                    fired++;
                }
            } catch (Exception ex) {
                cronJob.markError(ex.getMessage());
                cronJobRepository.save(cronJob);
            }
        }
        return fired;
    }

    private void fire(CronJob cronJob) {
        if (cronJob.getTargetType() == CronTargetType.SYNC_HOST) {
            syncHostUseCase.execute(cronJob.getTargetId());
            return;
        }
        enqueueBackupDatabaseUseCase.execute();
    }

    static boolean isDue(CronJob cronJob, Instant now) {
        CronExpression expression = CronExpression.parse(cronJob.getCronExpression());
        Instant anchor = cronJob.getLastFiredAt() != null ? cronJob.getLastFiredAt() : cronJob.getCreatedAt();
        ZonedDateTime next = expression.next(ZonedDateTime.ofInstant(anchor, ZoneId.systemDefault()));
        if (next == null) {
            return false;
        }
        return !next.toInstant().isAfter(now);
    }
}
