package com.atlas.platform.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Deployment {

    private final UUID id;
    private final UUID installationId;
    private final UUID applicationId;
    private final UUID hostId;
    private DeploymentStatus status;
    private final Instant startedAt;
    private Instant finishedAt;
    private String logs;

    public Deployment(
            UUID id,
            UUID installationId,
            UUID applicationId,
            UUID hostId,
            DeploymentStatus status,
            Instant startedAt,
            Instant finishedAt,
            String logs) {
        this.id = Objects.requireNonNull(id);
        this.installationId = Objects.requireNonNull(installationId);
        this.applicationId = Objects.requireNonNull(applicationId);
        this.hostId = Objects.requireNonNull(hostId);
        this.status = Objects.requireNonNull(status);
        this.startedAt = Objects.requireNonNull(startedAt);
        this.finishedAt = finishedAt;
        this.logs = logs;
    }

    public UUID getId() { return id; }
    public UUID getInstallationId() { return installationId; }
    public UUID getApplicationId() { return applicationId; }
    public UUID getHostId() { return hostId; }
    public DeploymentStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public String getLogs() { return logs; }
}
