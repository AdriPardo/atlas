package com.atlas.domain.service;

import com.atlas.domain.shared.DomainException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Deployable unit under a Project. Named ServiceUnit to avoid clash with Spring's {@code @Service}.
 */
@Getter
public class ServiceUnit {

    public static final String DEFAULT_NAME = "default";

    private final UUID id;
    private final UUID projectId;
    private String name;
    private String repositoryUrl;
    private String branch;
    private String composePath;
    private String domain;
    private String environment;
    private ServiceExposure exposure;
    private ServiceStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private ServiceUnit(
            UUID id,
            UUID projectId,
            String name,
            String repositoryUrl,
            String branch,
            String composePath,
            String domain,
            String environment,
            ServiceExposure exposure,
            ServiceStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.projectId = Objects.requireNonNull(projectId, "projectId is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        apply(name, repositoryUrl, branch, composePath, domain, environment, exposure, status, updatedAt);
    }

    public static ServiceUnit createDefault(
            UUID projectId,
            String repositoryUrl,
            String branch,
            String composePath,
            String domain) {
        Instant now = Instant.now();
        return new ServiceUnit(
                UUID.randomUUID(),
                projectId,
                DEFAULT_NAME,
                repositoryUrl,
                branch,
                composePath,
                domain,
                "default",
                ServiceExposure.PUBLIC,
                ServiceStatus.REGISTERED,
                now,
                now);
    }

    public static ServiceUnit create(
            UUID projectId,
            String name,
            String repositoryUrl,
            String branch,
            String composePath,
            String domain,
            String environment) {
        Instant now = Instant.now();
        return new ServiceUnit(
                UUID.randomUUID(),
                projectId,
                name,
                repositoryUrl,
                branch,
                composePath,
                domain,
                environment == null || environment.isBlank() ? "default" : environment,
                ServiceExposure.PUBLIC,
                ServiceStatus.REGISTERED,
                now,
                now);
    }

    public static ServiceUnit rehydrate(
            UUID id,
            UUID projectId,
            String name,
            String repositoryUrl,
            String branch,
            String composePath,
            String domain,
            String environment,
            ServiceExposure exposure,
            ServiceStatus status,
            Instant createdAt,
            Instant updatedAt) {
        return new ServiceUnit(
                id,
                projectId,
                name,
                repositoryUrl,
                branch,
                composePath,
                domain,
                environment,
                exposure == null ? ServiceExposure.PUBLIC : exposure,
                status,
                createdAt,
                updatedAt);
    }

    public void update(
            String name,
            String repositoryUrl,
            String branch,
            String composePath,
            String domain,
            String environment,
            ServiceStatus status) {
        apply(
                name,
                repositoryUrl,
                branch,
                composePath,
                domain,
                environment == null || environment.isBlank() ? "default" : environment,
                exposure,
                status,
                Instant.now());
    }

    public void updateStatus(ServiceStatus status) {
        apply(name, repositoryUrl, branch, composePath, domain, environment, exposure, status, Instant.now());
    }

    public void updateExposure(ServiceExposure exposure) {
        apply(
                name,
                repositoryUrl,
                branch,
                composePath,
                domain,
                environment,
                exposure == null ? ServiceExposure.PUBLIC : exposure,
                status,
                Instant.now());
    }

    public void updateDomain(String domain) {
        apply(name, repositoryUrl, branch, composePath, domain, environment, exposure, status, Instant.now());
    }

    private void apply(
            String name,
            String repositoryUrl,
            String branch,
            String composePath,
            String domain,
            String environment,
            ServiceExposure exposure,
            ServiceStatus status,
            Instant updatedAt) {
        this.name = requireText(name, "name");
        this.repositoryUrl = requireText(repositoryUrl, "repositoryUrl");
        this.branch = requireText(branch, "branch");
        // Optional when repo atlas.yml supplies runtime.composeFile (ADR-0014 phase C).
        this.composePath = composePath == null ? "" : composePath.trim();
        this.domain = domain == null ? "" : domain.trim();
        this.environment = requireText(environment, "environment");
        this.exposure = Objects.requireNonNull(exposure, "exposure is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    /** True when a legacy Compose path is stored (may still be overridden by atlas.yml at deploy). */
    public boolean hasComposePath() {
        return composePath != null && !composePath.isBlank();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainException(field + " is required");
        }
        return value.trim();
    }
}
