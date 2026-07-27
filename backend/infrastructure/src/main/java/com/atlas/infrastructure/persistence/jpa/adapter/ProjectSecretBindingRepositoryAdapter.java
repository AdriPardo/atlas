package com.atlas.infrastructure.persistence.jpa.adapter;

import com.atlas.application.port.out.ProjectSecretBindingRepositoryPort;
import com.atlas.domain.secret.ProjectSecretBinding;
import com.atlas.infrastructure.persistence.jpa.entity.ProjectSecretBindingJpaEntity;
import com.atlas.infrastructure.persistence.jpa.repository.ProjectSecretBindingJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectSecretBindingRepositoryAdapter implements ProjectSecretBindingRepositoryPort {

    private final ProjectSecretBindingJpaRepository repository;

    @Override
    public ProjectSecretBinding save(ProjectSecretBinding binding) {
        return toDomain(repository.save(toEntity(binding)));
    }

    @Override
    public Optional<ProjectSecretBinding> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<ProjectSecretBinding> findByProjectIdAndAlias(UUID projectId, String alias) {
        return repository.findByProjectIdAndAliasIgnoreCase(projectId, alias).map(this::toDomain);
    }

    @Override
    public List<ProjectSecretBinding> findByProjectId(UUID projectId) {
        return repository.findByProjectIdOrderByAliasAsc(projectId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByProjectIdAndSecretId(UUID projectId, UUID secretId) {
        return repository.existsByProjectIdAndSecretId(projectId, secretId);
    }

    @Override
    public boolean existsByProjectIdAndAlias(UUID projectId, String alias) {
        return repository.existsByProjectIdAndAliasIgnoreCase(projectId, alias);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    private ProjectSecretBinding toDomain(ProjectSecretBindingJpaEntity entity) {
        return ProjectSecretBinding.rehydrate(
                entity.getId(),
                entity.getProjectId(),
                entity.getSecretId(),
                entity.getAlias(),
                entity.getCreatedAt());
    }

    private ProjectSecretBindingJpaEntity toEntity(ProjectSecretBinding domain) {
        ProjectSecretBindingJpaEntity entity = new ProjectSecretBindingJpaEntity();
        entity.setId(domain.getId());
        entity.setProjectId(domain.getProjectId());
        entity.setSecretId(domain.getSecretId());
        entity.setAlias(domain.getAlias());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
