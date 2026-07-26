package com.atlas.domain.project;

import com.atlas.domain.organization.Organization;
import com.atlas.domain.shared.DomainException;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Project {

    private final UUID id;
    private final UUID organizationId;
    private String name;
    private String slug;
    private String description;
    private ProjectStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Project(
            UUID id,
            UUID organizationId,
            String name,
            String slug,
            String description,
            ProjectStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        apply(name, slug, description, status, updatedAt);
    }

    public static Project create(String name, String description) {
        Instant now = Instant.now();
        String trimmed = requireText(name, "name");
        return new Project(
                UUID.randomUUID(),
                Organization.DEFAULT_ID,
                trimmed,
                slugify(trimmed),
                description == null ? "" : description.trim(),
                ProjectStatus.REGISTERED,
                now,
                now);
    }

    public static Project rehydrate(
            UUID id,
            UUID organizationId,
            String name,
            String slug,
            String description,
            ProjectStatus status,
            Instant createdAt,
            Instant updatedAt) {
        return new Project(id, organizationId, name, slug, description, status, createdAt, updatedAt);
    }

    public void update(String name, String description, ProjectStatus status) {
        String trimmed = requireText(name, "name");
        apply(trimmed, slugify(trimmed), description, status, Instant.now());
    }

    public void updateStatus(ProjectStatus status) {
        apply(name, slug, description, status, Instant.now());
    }

    private void apply(
            String name, String slug, String description, ProjectStatus status, Instant updatedAt) {
        this.name = requireText(name, "name");
        this.slug = requireText(slug, "slug");
        this.description = description == null ? "" : description.trim();
        this.status = Objects.requireNonNull(status, "status is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    public static String slugify(String name) {
        String slug = name.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            throw new DomainException("name must yield a non-empty slug");
        }
        return slug;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainException(field + " is required");
        }
        return value.trim();
    }
}
