package com.atlas.platform.infrastructure.persistence.entity;

import com.atlas.platform.domain.model.ApplicationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "applications")
public class ApplicationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "installation_id", nullable = false)
    private UUID installationId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "repository_url", nullable = false, length = 500)
    private String repositoryUrl;

    @Column(nullable = false, length = 120)
    private String branch;

    @Column(name = "compose_path", nullable = false, length = 255)
    private String composePath;

    @Column(length = 255)
    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ApplicationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
