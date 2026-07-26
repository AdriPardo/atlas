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
            ServiceStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.projectId = Objects.requireNonNull(projectId, "projectId is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        apply(name, repositoryUrl, branch, composePath, domain, environment, status, updatedAt);
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
                status,
                Instant.now());
    }

    public void updateStatus(ServiceStatus status) {
        apply(name, repositoryUrl, branch, composePath, domain, environment, status, Instant.now());
    }

    private void apply(
            String name,
            String repositoryUrl,
            String branch,
            String composePath,
            String domain,
            String environment,
            ServiceStatus status,
            Instant updatedAt) {
        this.name = requireText(name, "name");
        this.repositoryUrl = requireText(repositoryUrl, "repositoryUrl");
        this.branch = requireText(branch, "branch");
        this.composePath = requireText(composePath, "composePath");
        this.domain = domain == null ? "" : domain.trim();
        this.environment = requireText(environment, "environment");
        this.status = Objects.requireNonNull(status, "status is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainException(field + " is required");
        }
        return value.trim();
    }
}
