package com.atlas.infrastructure.persistence.jpa.adapter;

import com.atlas.application.port.out.ProjectMembershipRepositoryPort;
import com.atlas.domain.access.ProjectMemberRole;
import com.atlas.domain.access.ProjectMembership;
import com.atlas.infrastructure.persistence.jpa.entity.ProjectMembershipJpaEntity;
import com.atlas.infrastructure.persistence.jpa.repository.ProjectMembershipJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectMembershipRepositoryAdapter implements ProjectMembershipRepositoryPort {

    private final ProjectMembershipJpaRepository repository;

    @Override
    public ProjectMembership save(ProjectMembership membership) {
        return toDomain(repository.save(toEntity(membership)));
    }

    @Override
    public Optional<ProjectMembership> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<ProjectMembership> findByProjectIdAndUserId(UUID projectId, UUID userId) {
        return repository.findByProjectIdAndUserId(projectId, userId).map(this::toDomain);
    }

    @Override
    public List<ProjectMembership> findByProjectId(UUID projectId) {
        return repository.findByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<UUID> findProjectIdsByUserId(UUID userId) {
        return repository.findProjectIdsByUserId(userId);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    private ProjectMembership toDomain(ProjectMembershipJpaEntity entity) {
        return ProjectMembership.rehydrate(
                entity.getId(),
                entity.getProjectId(),
                entity.getUserId(),
                ProjectMemberRole.valueOf(entity.getRole()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private ProjectMembershipJpaEntity toEntity(ProjectMembership domain) {
        ProjectMembershipJpaEntity entity = new ProjectMembershipJpaEntity();
        entity.setId(domain.getId());
        entity.setProjectId(domain.getProjectId());
        entity.setUserId(domain.getUserId());
        entity.setRole(domain.getRole().name());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
