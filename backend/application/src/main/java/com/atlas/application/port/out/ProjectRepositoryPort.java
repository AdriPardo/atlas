package com.atlas.application.port.out;

import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.project.Project;
import com.atlas.domain.project.ProjectStatus;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepositoryPort {

    Project save(Project project);

    Optional<Project> findById(UUID id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    PageResult<Project> search(String name, ProjectStatus status, PageQuery pageQuery);

    void deleteById(UUID id);

    long count();
}
