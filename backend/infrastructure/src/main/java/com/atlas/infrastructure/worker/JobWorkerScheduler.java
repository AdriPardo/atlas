package com.atlas.infrastructure.worker;

import com.atlas.application.backup.ExecuteBackupDatabaseJobUseCase;
import com.atlas.application.deployment.ExecuteDeployServiceJobUseCase;
import com.atlas.application.host.ExecuteSyncHostJobUseCase;
import com.atlas.application.job.ClaimJobsUseCase;
import com.atlas.application.job.CompleteJobUseCase;
import com.atlas.application.job.FailJobUseCase;
import com.atlas.application.job.RecoverStaleJobsUseCase;
import com.atlas.application.port.out.JobRepositoryPort;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobType;
import com.atlas.infrastructure.config.AtlasProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "atlas.worker.enabled", havingValue = "true", matchIfMissing = true)
public class JobWorkerScheduler {

    private final ClaimJobsUseCase claimJobsUseCase;
    private final CompleteJobUseCase completeJobUseCase;
    private final FailJobUseCase failJobUseCase;
    private final RecoverStaleJobsUseCase recoverStaleJobsUseCase;
    private final JobRepositoryPort jobRepository;
    private final ExecuteSyncHostJobUseCase executeSyncHostJobUseCase;
    private final ExecuteDeployServiceJobUseCase executeDeployServiceJobUseCase;
    private final ExecuteBackupDatabaseJobUseCase executeBackupDatabaseJobUseCase;
    private final AtlasProperties atlasProperties;
    private final ObjectMapper objectMapper;

    @PostConstruct
    void reclaimOnStartup() {
        try {
            int recovered = recoverStaleJobsUseCase.execute(staleTimeout());
            if (recovered > 0) {
                log.warn("Startup reclaim: marked {} stale RUNNING job(s) as FAILED", recovered);
            }
        } catch (Exception ex) {
            log.warn("Startup stale job reclaim failed: {}", ex.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${atlas.worker.poll-interval-ms:2000}")
    public void poll() {
        List<Job> claimed = claimJobsUseCase.execute(
                atlasProperties.getWorker().getId(), atlasProperties.getWorker().getBatchSize());
        for (Job job : claimed) {
            process(job);
        }
    }

    @Scheduled(fixedDelayString = "${atlas.worker.stale-reclaim-interval-ms:30000}")
    public void reclaimStale() {
        try {
            int recovered = recoverStaleJobsUseCase.execute(staleTimeout());
            if (recovered > 0) {
                log.warn("Periodic reclaim: marked {} stale RUNNING job(s) as FAILED", recovered);
            }
        } catch (Exception ex) {
            log.warn("Periodic stale job reclaim failed: {}", ex.getMessage());
        }
    }

    private void process(Job job) {
        String workerId = atlasProperties.getWorker().getId();
        long heartbeatSeconds = Math.max(5, atlasProperties.getWorker().getHeartbeatIntervalSeconds());
        ScheduledExecutorService heartbeatExecutor =
                Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "atlas-job-heartbeat-" + job.getId());
                    t.setDaemon(true);
                    return t;
                });
        ScheduledFuture<?> heartbeat = heartbeatExecutor.scheduleAtFixedRate(
                () -> {
                    try {
                        jobRepository.heartbeat(job.getId(), workerId);
                    } catch (Exception ex) {
                        log.debug("Heartbeat failed for job {}: {}", job.getId(), ex.getMessage());
                    }
                },
                heartbeatSeconds,
                heartbeatSeconds,
                TimeUnit.SECONDS);
        try {
            JsonNode payload = objectMapper.readTree(job.getPayload());
            if (job.getType() == JobType.SYNC_HOST) {
                UUID hostId = UUID.fromString(payload.get("hostId").asText());
                executeSyncHostJobUseCase.execute(hostId);
            } else if (job.getType() == JobType.DEPLOY_SERVICE) {
                UUID deploymentId = UUID.fromString(payload.get("deploymentId").asText());
                executeDeployServiceJobUseCase.execute(deploymentId);
            } else if (job.getType() == JobType.BACKUP_DATABASE) {
                executeBackupDatabaseJobUseCase.execute();
            } else {
                throw new IllegalStateException("Unsupported job type: " + job.getType());
            }
            completeJobUseCase.execute(job.getId());
        } catch (Exception ex) {
            log.warn("Job {} failed: {}", job.getId(), ex.getMessage());
            failJobUseCase.execute(job.getId(), ex.getMessage());
        } finally {
            heartbeat.cancel(false);
            heartbeatExecutor.shutdownNow();
        }
    }

    private Duration staleTimeout() {
        long seconds = atlasProperties.getWorker().getStaleTimeoutSeconds();
        if (seconds <= 0) {
            return Duration.ofMinutes(30);
        }
        return Duration.ofSeconds(seconds);
    }
}
