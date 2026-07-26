package com.atlas.platform.infrastructure.persistence.entity;

import com.atlas.platform.domain.model.DeploymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "deployments")
public class DeploymentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "installation_id", nullable = false)
    private UUID installationId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DeploymentStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Lob
    private String logs;
}
