package com.atlas.infrastructure.persistence.jpa.entity;

import com.atlas.domain.host.ConnectionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hosts")
public class HostJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String hostname;

    @Column(nullable = false, length = 64)
    private String ip;

    @Column(name = "operating_system", nullable = false, length = 150)
    private String operatingSystem;

    @Column(name = "docker_version", nullable = false, length = 100)
    private String dockerVersion;

    @Column(nullable = false)
    private boolean online;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_type", nullable = false, length = 16)
    private ConnectionType connectionType;

    @Column(name = "ssh_user", length = 128)
    private String sshUser;

    @Column(name = "ssh_port", nullable = false)
    private int sshPort;

    @Column(name = "ssh_private_key_secret_id")
    private UUID sshPrivateKeySecretId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public String getDockerVersion() {
        return dockerVersion;
    }

    public void setDockerVersion(String dockerVersion) {
        this.dockerVersion = dockerVersion;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public ConnectionType getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(ConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    public String getSshUser() {
        return sshUser;
    }

    public void setSshUser(String sshUser) {
        this.sshUser = sshUser;
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    public UUID getSshPrivateKeySecretId() {
        return sshPrivateKeySecretId;
    }

    public void setSshPrivateKeySecretId(UUID sshPrivateKeySecretId) {
        this.sshPrivateKeySecretId = sshPrivateKeySecretId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
