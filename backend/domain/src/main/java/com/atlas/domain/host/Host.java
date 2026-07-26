package com.atlas.domain.host;

import com.atlas.domain.shared.DomainException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Host {

    private final UUID id;
    private String hostname;
    private String ip;
    private String operatingSystem;
    private String dockerVersion;
    private boolean online;
    private final Instant createdAt;
    private Instant updatedAt;

    private Host(
            UUID id,
            String hostname,
            String ip,
            String operatingSystem,
            String dockerVersion,
            boolean online,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        apply(hostname, ip, operatingSystem, dockerVersion, online, updatedAt);
    }

    public static Host create(
            String hostname,
            String ip,
            String operatingSystem,
            String dockerVersion,
            boolean online) {
        Instant now = Instant.now();
        return new Host(
                UUID.randomUUID(),
                hostname,
                ip,
                operatingSystem,
                dockerVersion,
                online,
                now,
                now);
    }

    public static Host rehydrate(
            UUID id,
            String hostname,
            String ip,
            String operatingSystem,
            String dockerVersion,
            boolean online,
            Instant createdAt,
            Instant updatedAt) {
        return new Host(
                id, hostname, ip, operatingSystem, dockerVersion, online, createdAt, updatedAt);
    }

    public void update(
            String hostname,
            String ip,
            String operatingSystem,
            String dockerVersion,
            boolean online) {
        apply(hostname, ip, operatingSystem, dockerVersion, online, Instant.now());
    }

    private void apply(
            String hostname,
            String ip,
            String operatingSystem,
            String dockerVersion,
            boolean online,
            Instant updatedAt) {
        this.hostname = requireText(hostname, "hostname");
        this.ip = requireText(ip, "ip");
        this.operatingSystem = requireText(operatingSystem, "operatingSystem");
        this.dockerVersion = dockerVersion == null ? "" : dockerVersion.trim();
        this.online = online;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainException(field + " is required");
        }
        return value.trim();
    }
}
