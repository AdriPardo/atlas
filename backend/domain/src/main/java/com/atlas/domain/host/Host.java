package com.atlas.domain.host;

import com.atlas.domain.runtime.RuntimeCapability;
import com.atlas.domain.shared.DomainException;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class Host {

    private final UUID id;
    private String hostname;
    private String ip;
    private String operatingSystem;
    private String dockerVersion;
    private boolean online;
    private ConnectionType connectionType;
    private String sshUser;
    private int sshPort;
    private UUID sshPrivateKeySecretId;
    private final Instant createdAt;
    private Instant updatedAt;

    private Host(
            UUID id,
            String hostname,
            String ip,
            String operatingSystem,
            String dockerVersion,
            boolean online,
            ConnectionType connectionType,
            String sshUser,
            int sshPort,
            UUID sshPrivateKeySecretId,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        apply(
                hostname,
                ip,
                operatingSystem,
                dockerVersion,
                online,
                connectionType,
                sshUser,
                sshPort,
                sshPrivateKeySecretId,
                updatedAt);
    }

    public static Host create(
            String hostname,
            String ip,
            String operatingSystem,
            String dockerVersion,
            boolean online,
            ConnectionType connectionType,
            String sshUser,
            Integer sshPort,
            UUID sshPrivateKeySecretId) {
        Instant now = Instant.now();
        return new Host(
                UUID.randomUUID(),
                hostname,
                ip,
                operatingSystem,
                dockerVersion,
                online,
                connectionType == null ? ConnectionType.LOCAL : connectionType,
                sshUser,
                sshPort == null ? 22 : sshPort,
                sshPrivateKeySecretId,
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
            ConnectionType connectionType,
            String sshUser,
            int sshPort,
            UUID sshPrivateKeySecretId,
            Instant createdAt,
            Instant updatedAt) {
        return new Host(
                id,
                hostname,
                ip,
                operatingSystem,
                dockerVersion,
                online,
                connectionType == null ? ConnectionType.LOCAL : connectionType,
                sshUser,
                sshPort,
                sshPrivateKeySecretId,
                createdAt,
                updatedAt);
    }

    public void update(
            String hostname,
            String ip,
            String operatingSystem,
            String dockerVersion,
            boolean online,
            ConnectionType connectionType,
            String sshUser,
            Integer sshPort,
            UUID sshPrivateKeySecretId) {
        apply(
                hostname,
                ip,
                operatingSystem,
                dockerVersion,
                online,
                connectionType == null ? this.connectionType : connectionType,
                sshUser,
                sshPort == null ? this.sshPort : sshPort,
                sshPrivateKeySecretId,
                Instant.now());
    }

    public void applyInspection(String operatingSystem, String dockerVersion, boolean online) {
        apply(
                hostname,
                ip,
                operatingSystem,
                dockerVersion,
                online,
                connectionType,
                sshUser,
                sshPort,
                sshPrivateKeySecretId,
                Instant.now());
    }

    private void apply(
            String hostname,
            String ip,
            String operatingSystem,
            String dockerVersion,
            boolean online,
            ConnectionType connectionType,
            String sshUser,
            int sshPort,
            UUID sshPrivateKeySecretId,
            Instant updatedAt) {
        this.hostname = requireText(hostname, "hostname");
        this.ip = requireText(ip, "ip");
        this.operatingSystem = requireText(operatingSystem, "operatingSystem");
        this.dockerVersion = dockerVersion == null ? "" : dockerVersion.trim();
        this.online = online;
        this.connectionType = Objects.requireNonNull(connectionType, "connectionType is required");
        this.sshUser = sshUser == null || sshUser.isBlank() ? null : sshUser.trim();
        if (sshPort <= 0 || sshPort > 65535) {
            throw new DomainException("sshPort must be between 1 and 65535");
        }
        this.sshPort = sshPort;
        this.sshPrivateKeySecretId = sshPrivateKeySecretId;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");

        if (this.connectionType == ConnectionType.SSH && this.sshUser == null) {
            throw new DomainException("sshUser is required for SSH hosts");
        }
    }

    /**
     * Runtime capacity tags (ADR-0014 phase D). Today every Atlas host advertises
     * {@code compose}; later sync/provision may add {@code podman}/{@code k8s}/….
     */
    public Set<RuntimeCapability> runtimeCapabilities() {
        Set<RuntimeCapability> caps = new LinkedHashSet<>();
        caps.add(RuntimeCapability.COMPOSE);
        return Set.copyOf(caps);
    }

    public boolean supportsRuntime(RuntimeCapability capability) {
        return capability != null && runtimeCapabilities().contains(capability);
    }

    /** Wire-format tags for API / placement filters ({@code compose}, …). */
    public Set<String> runtimeCapabilityTags() {
        return runtimeCapabilities().stream()
                .map(RuntimeCapability::tag)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainException(field + " is required");
        }
        return value.trim();
    }
}
