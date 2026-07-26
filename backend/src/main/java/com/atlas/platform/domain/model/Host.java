package com.atlas.platform.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Host {

    private final UUID id;
    private final UUID installationId;
    private String hostname;
    private String ip;
    private String operatingSystem;
    private String dockerVersion;
    private boolean online;
    private final Instant createdAt;

    public Host(
            UUID id,
            UUID installationId,
            String hostname,
            String ip,
            String operatingSystem,
            String dockerVersion,
            boolean online,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.installationId = Objects.requireNonNull(installationId);
        this.hostname = Objects.requireNonNull(hostname);
        this.ip = Objects.requireNonNull(ip);
        this.operatingSystem = operatingSystem;
        this.dockerVersion = dockerVersion;
        this.online = online;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public UUID getId() { return id; }
    public UUID getInstallationId() { return installationId; }
    public String getHostname() { return hostname; }
    public String getIp() { return ip; }
    public String getOperatingSystem() { return operatingSystem; }
    public String getDockerVersion() { return dockerVersion; }
    public boolean isOnline() { return online; }
    public Instant getCreatedAt() { return createdAt; }
}
