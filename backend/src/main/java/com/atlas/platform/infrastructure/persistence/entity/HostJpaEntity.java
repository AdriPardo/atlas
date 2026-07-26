package com.atlas.platform.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hosts")
public class HostJpaEntity {

    @Id
    private UUID id;

    @Column(name = "installation_id", nullable = false)
    private UUID installationId;

    @Column(nullable = false, length = 255)
    private String hostname;

    @Column(nullable = false, length = 64)
    private String ip;

    @Column(name = "operating_system", length = 120)
    private String operatingSystem;

    @Column(name = "docker_version", length = 80)
    private String dockerVersion;

    @Column(nullable = false)
    private boolean online;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
