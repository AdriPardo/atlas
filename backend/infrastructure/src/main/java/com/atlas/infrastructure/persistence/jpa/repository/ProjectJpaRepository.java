package com.atlas.infrastructure.persistence.jpa.repository;

import com.atlas.infrastructure.persistence.jpa.entity.ProjectJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProjectJpaRepository
        extends JpaRepository<ProjectJpaEntity, UUID>, JpaSpecificationExecutor<ProjectJpaEntity> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, UUID id);
}
