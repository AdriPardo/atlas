package com.atlas.domain.application;

import com.atlas.domain.shared.DomainException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Application {

    private final UUID id;
    private String name;
    private String description;
    private String repositoryUrl;
    private String branch;
    private String composePath;
    private String domain;
    private ApplicationStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Application(
            UUID id,
            String name,
            String description,
            String repositoryUrl,
            String branch,
            String composePath,
            String domain,
            ApplicationStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        apply(
                name,
                description,
                repositoryUrl,
                branch,
                composePath,
                domain,
                status,
                updatedAt);
    }

    public static Application create(
            String name,
            String description,
            String repositoryUrl,
            String branch,
            String composePath,
            String domain) {
        Instant now = Instant.now();
        return new Application(
                UUID.randomUUID(),
                name,
                description,
                repositoryUrl,
                branch,
                composePath,
                domain,
                ApplicationStatus.REGISTERED,
                now,
                now);
    }

    public static Application rehydrate(
            UUID id,
            String name,
            String description,
            String repositoryUrl,
            String branch,
            String composePath,
            String domain,
            ApplicationStatus status,
            Instant createdAt,
            Instant updatedAt) {
        return new Application(
                id,
                name,
                description,
                repositoryUrl,
                branch,
                composePath,
                domain,
                status,
                createdAt,
                updatedAt);
    }

    public void update(
            String name,
            String description,
            String repositoryUrl,
            String branch,
            String composePath,
            String domain,
            ApplicationStatus status) {
        apply(name, description, repositoryUrl, branch, composePath, domain, status, Instant.now());
    }

    private void apply(
            String name,
            String description,
            String repositoryUrl,
            String branch,
            String composePath,
            String domain,
            ApplicationStatus status,
            Instant updatedAt) {
        this.name = requireText(name, "name");
        this.description = description == null ? "" : description.trim();
        this.repositoryUrl = requireText(repositoryUrl, "repositoryUrl");
        this.branch = requireText(branch, "branch");
        this.composePath = requireText(composePath, "composePath");
        this.domain = domain == null ? "" : domain.trim();
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
