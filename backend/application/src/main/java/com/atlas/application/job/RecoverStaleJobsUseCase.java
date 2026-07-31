package com.atlas.application.job;

import com.atlas.application.observability.EvaluateProductAlertsUseCase;
import com.atlas.application.port.out.BillingMeterPort;
import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.JobRepositoryPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.deployment.DeploymentStatus;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobType;
import com.atlas.domain.observability.AlertEventType;
import com.atlas.domain.project.Project;
import com.atlas.domain.project.ProjectStatus;
import com.atlas.domain.service.ServiceStatus;
import com.atlas.domain.service.ServiceUnit;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recovers jobs left in {@code RUNNING} after a worker crash. Leases older than the configured
 * stale timeout are marked {@code FAILED} (locks cleared) so new deploys can be enqueued. Related
 * deployments stuck in PENDING/RUNNING and services/projects stuck in DEPLOYING are failed too.
 */
@Service
@RequiredArgsConstructor
public class RecoverStaleJobsUseCase {

    public static final String STALE_ERROR_PREFIX = "Stale RUNNING lease expired";

    private static final Pattern DEPLOYMENT_ID =
            Pattern.compile("\"deploymentId\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");

    private static final int DEFAULT_BATCH = 50;

    private final JobRepositoryPort jobRepository;
    private final DeploymentRepositoryPort deploymentRepository;
    private final ServiceRepositoryPort serviceRepository;
    private final ProjectRepositoryPort projectRepository;
    private final EvaluateProductAlertsUseCase evaluateProductAlertsUseCase;
    private final BillingMeterPort billingMeter;

    @Transactional
    public int execute(Duration staleTimeout) {
        Duration timeout = staleTimeout == null || staleTimeout.isNegative() || staleTimeout.isZero()
                ? Duration.ofMinutes(30)
                : staleTimeout;
        Instant cutoff = Instant.now().minus(timeout);
        List<Job> stale = jobRepository.findAndLockStaleRunning(cutoff, DEFAULT_BATCH);
        int recovered = 0;
        for (Job job : stale) {
            String message = STALE_ERROR_PREFIX
                    + " (worker crash or lost heartbeat); locked_by="
                    + (job.getLockedBy() == null ? "unknown" : job.getLockedBy())
                    + ", locked_at="
                    + job.getLockedAt();
            job.markFailed(message);
            Job saved = jobRepository.save(job);
            JobMinutesMetering.record(billingMeter, saved);
            evaluateProductAlertsUseCase.execute(
                    AlertEventType.JOB_FAILED,
                    null,
                    "Job " + saved.getType() + " failed: " + message,
                    "job",
                    saved.getId());
            if (saved.getType() == JobType.DEPLOY_SERVICE) {
                cascadeDeployFailure(saved, message);
            }
            recovered++;
        }
        return recovered;
    }

    private void cascadeDeployFailure(Job job, String message) {
        Optional<UUID> deploymentId = extractDeploymentId(job.getPayload());
        if (deploymentId.isEmpty()) {
            return;
        }
        Deployment deployment = deploymentRepository.findById(deploymentId.get()).orElse(null);
        if (deployment == null) {
            return;
        }
        if (deployment.getStatus() == DeploymentStatus.PENDING
                || deployment.getStatus() == DeploymentStatus.RUNNING) {
            deployment.markFailed("ERROR: " + message);
            deploymentRepository.save(deployment);
            evaluateProductAlertsUseCase.execute(
                    AlertEventType.DEPLOY_FAILED,
                    null,
                    "Deploy failed: " + message,
                    "deployment",
                    deployment.getId());
        }

        ServiceUnit service = serviceRepository.findById(deployment.getServiceId()).orElse(null);
        if (service == null) {
            return;
        }
        if (service.getStatus() == ServiceStatus.DEPLOYING) {
            service.updateStatus(ServiceStatus.FAILED);
            serviceRepository.save(service);
        }
        Project project = projectRepository.findById(service.getProjectId()).orElse(null);
        if (project != null && project.getStatus() == ProjectStatus.DEPLOYING) {
            project.updateStatus(ProjectStatus.FAILED);
            projectRepository.save(project);
        }
    }

    static Optional<UUID> extractDeploymentId(String payload) {
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = DEPLOYMENT_ID.matcher(payload);
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(matcher.group(1)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
