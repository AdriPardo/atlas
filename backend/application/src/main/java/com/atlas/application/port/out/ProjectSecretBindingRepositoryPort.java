package com.atlas.application.port.out;

import com.atlas.domain.secret.ProjectSecretBinding;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectSecretBindingRepositoryPort {

    ProjectSecretBinding save(ProjectSecretBinding binding);

    Optional<ProjectSecretBinding> findById(UUID id);

    Optional<ProjectSecretBinding> findByProjectIdAndAlias(UUID projectId, String alias);

    List<ProjectSecretBinding> findByProjectId(UUID projectId);

    boolean existsByProjectIdAndSecretId(UUID projectId, UUID secretId);

    boolean existsByProjectIdAndAlias(UUID projectId, String alias);

    void deleteById(UUID id);
}
