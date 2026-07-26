package com.atlas.domain.pipeline;

import com.atlas.domain.deployment.DeploymentStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class PipelineRun {

    private final UUID id;
    private final UUID pipelineId;
    private PipelineRunStatus status;
    private final String triggeredBy;
    private UUID deploymentId;
    private UUID jobId;
    private Instant startedAt;
    private Instant finishedAt;
    private String lastError;
    private final Instant createdAt;
    private Instant updatedAt;

    private PipelineRun(
            UUID id,
            UUID pipelineId,
            PipelineRunStatus status,
            String triggeredBy,
            UUID deploymentId,
            UUID jobId,
            Instant startedAt,
            Instant finishedAt,
            String lastError,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.pipelineId = Objects.requireNonNull(pipelineId, "pipelineId is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.triggeredBy = triggeredBy == null || triggeredBy.isBlank() ? "manual" : triggeredBy.trim();
        this.deploymentId = deploymentId;
        this.jobId = jobId;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.lastError = lastError;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    public static PipelineRun start(UUID pipelineId, String triggeredBy) {
        Instant now = Instant.now();
        return new PipelineRun(
                UUID.randomUUID(),
                pipelineId,
                PipelineRunStatus.PENDING,
                triggeredBy,
                null,
                null,
                null,
                null,
                null,
                now,
                now);
    }

    public static PipelineRun rehydrate(
            UUID id,
            UUID pipelineId,
            PipelineRunStatus status,
            String triggeredBy,
            UUID deploymentId,
            UUID jobId,
            Instant startedAt,
            Instant finishedAt,
            String lastError,
            Instant createdAt,
            Instant updatedAt) {
        return new PipelineRun(
                id,
                pipelineId,
                status,
                triggeredBy,
                deploymentId,
                jobId,
                startedAt,
                finishedAt,
                lastError,
                createdAt,
                updatedAt);
    }

    public void markRunning(UUID deploymentId, UUID jobId) {
        this.deploymentId = Objects.requireNonNull(deploymentId, "deploymentId is required");
        this.jobId = Objects.requireNonNull(jobId, "jobId is required");
        this.status = PipelineRunStatus.RUNNING;
        this.startedAt = Instant.now();
        this.updatedAt = this.startedAt;
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.status = PipelineRunStatus.FAILED;
        this.finishedAt = Instant.now();
        this.updatedAt = this.finishedAt;
        this.lastError = error == null ? "Pipeline run failed" : error;
    }

    public void syncFromDeployment(DeploymentStatus deploymentStatus, String deploymentErrorHint) {
        if (status == PipelineRunStatus.SUCCEEDED
                || status == PipelineRunStatus.FAILED
                || status == PipelineRunStatus.CANCELLED) {
            return;
        }
        if (deploymentStatus == DeploymentStatus.SUCCEEDED) {
            this.status = PipelineRunStatus.SUCCEEDED;
            this.finishedAt = Instant.now();
            this.updatedAt = this.finishedAt;
            this.lastError = null;
        } else if (deploymentStatus == DeploymentStatus.FAILED
                || deploymentStatus == DeploymentStatus.CANCELLED) {
            this.status = deploymentStatus == DeploymentStatus.CANCELLED
                    ? PipelineRunStatus.CANCELLED
                    : PipelineRunStatus.FAILED;
            this.finishedAt = Instant.now();
            this.updatedAt = this.finishedAt;
            this.lastError = deploymentErrorHint;
        } else if (deploymentStatus == DeploymentStatus.RUNNING
                || deploymentStatus == DeploymentStatus.PENDING) {
            this.status = PipelineRunStatus.RUNNING;
            this.updatedAt = Instant.now();
        }
    }
}
