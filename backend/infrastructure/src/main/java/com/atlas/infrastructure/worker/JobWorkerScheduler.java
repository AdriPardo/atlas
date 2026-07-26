package com.atlas.infrastructure.worker;

import com.atlas.application.deployment.ExecuteDeployServiceJobUseCase;
import com.atlas.application.host.ExecuteSyncHostJobUseCase;
import com.atlas.application.job.ClaimJobsUseCase;
import com.atlas.application.job.CompleteJobUseCase;
import com.atlas.application.job.FailJobUseCase;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobType;
import com.atlas.infrastructure.config.AtlasProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
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
    private final ExecuteSyncHostJobUseCase executeSyncHostJobUseCase;
    private final ExecuteDeployServiceJobUseCase executeDeployServiceJobUseCase;
    private final AtlasProperties atlasProperties;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${atlas.worker.poll-interval-ms:2000}")
    public void poll() {
        List<Job> claimed = claimJobsUseCase.execute(
                atlasProperties.getWorker().getId(), atlasProperties.getWorker().getBatchSize());
        for (Job job : claimed) {
            process(job);
        }
    }

    private void process(Job job) {
        try {
            JsonNode payload = objectMapper.readTree(job.getPayload());
            if (job.getType() == JobType.SYNC_HOST) {
                UUID hostId = UUID.fromString(payload.get("hostId").asText());
                executeSyncHostJobUseCase.execute(hostId);
            } else if (job.getType() == JobType.DEPLOY_SERVICE) {
                UUID deploymentId = UUID.fromString(payload.get("deploymentId").asText());
                executeDeployServiceJobUseCase.execute(deploymentId);
            } else {
                throw new IllegalStateException("Unsupported job type: " + job.getType());
            }
            completeJobUseCase.execute(job.getId());
        } catch (Exception ex) {
            log.warn("Job {} failed: {}", job.getId(), ex.getMessage());
            failJobUseCase.execute(job.getId(), ex.getMessage());
        }
    }
}
