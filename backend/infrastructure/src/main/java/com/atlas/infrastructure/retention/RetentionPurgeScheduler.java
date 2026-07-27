package com.atlas.infrastructure.retention;

import com.atlas.application.retention.PurgeRetentionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "atlas.retention.enabled", havingValue = "true", matchIfMissing = true)
public class RetentionPurgeScheduler {

    private final PurgeRetentionUseCase purgeRetentionUseCase;

    @Scheduled(cron = "${atlas.retention.cron:0 0 3 * * *}")
    public void purge() {
        var result = purgeRetentionUseCase.execute();
        if (result.ran()) {
            log.info(
                    "Retention purge complete: deletedJobs={}, deletedPipelineRuns={}",
                    result.deletedJobs(),
                    result.deletedPipelineRuns());
        }
    }
}
