package com.atlas.application.port.out;

import com.atlas.domain.access.ProjectMembership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMembershipRepositoryPort {

    ProjectMembership save(ProjectMembership membership);

    Optional<ProjectMembership> findById(UUID id);

    Optional<ProjectMembership> findByProjectIdAndUserId(UUID projectId, UUID userId);

    List<ProjectMembership> findByProjectId(UUID projectId);

    List<UUID> findProjectIdsByUserId(UUID userId);

    void deleteById(UUID id);
}
