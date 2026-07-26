package com.atlas.domain.access;

import com.atlas.domain.shared.DomainException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class ProjectMembership {

    private final UUID id;
    private final UUID projectId;
    private final UUID userId;
    private ProjectMemberRole role;
    private final Instant createdAt;
    private Instant updatedAt;

    private ProjectMembership(
            UUID id,
            UUID projectId,
            UUID userId,
            ProjectMemberRole role,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.projectId = Objects.requireNonNull(projectId);
        this.userId = Objects.requireNonNull(userId);
        this.role = Objects.requireNonNull(role);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static ProjectMembership create(UUID projectId, UUID userId, ProjectMemberRole role) {
        Instant now = Instant.now();
        return new ProjectMembership(UUID.randomUUID(), projectId, userId, role, now, now);
    }

    public static ProjectMembership rehydrate(
            UUID id,
            UUID projectId,
            UUID userId,
            ProjectMemberRole role,
            Instant createdAt,
            Instant updatedAt) {
        return new ProjectMembership(id, projectId, userId, role, createdAt, updatedAt);
    }

    public void updateRole(ProjectMemberRole role) {
        if (role == null) {
            throw new DomainException("role is required");
        }
        this.role = role;
        this.updatedAt = Instant.now();
    }
}
