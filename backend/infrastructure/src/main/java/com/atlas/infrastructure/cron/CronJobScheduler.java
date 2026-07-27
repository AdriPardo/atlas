package com.atlas.infrastructure.cron;

import com.atlas.application.cron.TickCronJobsUseCase;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "atlas.worker.enabled", havingValue = "true", matchIfMissing = true)
public class CronJobScheduler {

    private final TickCronJobsUseCase tickCronJobsUseCase;

    @Scheduled(fixedDelayString = "${atlas.cron.poll-interval-ms:15000}")
    public void tick() {
        int fired = tickCronJobsUseCase.execute(Instant.now());
        if (fired > 0) {
            log.info("Cron tick fired {} job(s)", fired);
        }
    }
}
