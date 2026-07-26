package com.atlas.platform.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Application {

    private final UUID id;
    private final UUID installationId;
    private String name;
    private String description;
    private String repositoryUrl;
    private String branch;
    private String composePath;
    private String domain;
    private ApplicationStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Application(
            UUID id,
            UUID installationId,
            String name,
            String description,
            String repositoryUrl,
            String branch,
            String composePath,
            String domain,
            ApplicationStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.installationId = Objects.requireNonNull(installationId);
        this.name = requireText(name, "name");
        this.description = description;
        this.repositoryUrl = requireText(repositoryUrl, "repositoryUrl");
        this.branch = requireText(branch, "branch");
        this.composePath = requireText(composePath, "composePath");
        this.domain = domain;
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static Application create(
            UUID installationId,
            String name,
            String description,
            String repositoryUrl,
            String branch,
            String composePath,
            String domain) {
        Instant now = Instant.now();
        return new Application(
                UUID.randomUUID(),
                installationId,
                name,
                description,
                repositoryUrl,
                branch,
                composePath,
                domain,
                ApplicationStatus.DRAFT,
                now,
                now);
    }

    public void update(
            String name,
            String description,
            String repositoryUrl,
            String branch,
            String composePath,
            String domain,
            ApplicationStatus status) {
        this.name = requireText(name, "name");
        this.description = description;
        this.repositoryUrl = requireText(repositoryUrl, "repositoryUrl");
        this.branch = requireText(branch, "branch");
        this.composePath = requireText(composePath, "composePath");
        this.domain = domain;
        this.status = Objects.requireNonNull(status);
        this.updatedAt = Instant.now();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    public UUID getId() { return id; }
    public UUID getInstallationId() { return installationId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getRepositoryUrl() { return repositoryUrl; }
    public String getBranch() { return branch; }
    public String getComposePath() { return composePath; }
    public String getDomain() { return domain; }
    public ApplicationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
