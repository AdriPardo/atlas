package com.atlas.infrastructure.persistence.jpa.repository;

import com.atlas.infrastructure.persistence.jpa.entity.ProjectSecretBindingJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectSecretBindingJpaRepository
        extends JpaRepository<ProjectSecretBindingJpaEntity, UUID> {

    @Query(
            "select b from ProjectSecretBindingJpaEntity b"
                    + " where b.projectId = :projectId and lower(b.alias) = lower(:alias)")
    Optional<ProjectSecretBindingJpaEntity> findByProjectIdAndAliasIgnoreCase(
            @Param("projectId") UUID projectId, @Param("alias") String alias);

    List<ProjectSecretBindingJpaEntity> findByProjectIdOrderByAliasAsc(UUID projectId);

    boolean existsByProjectIdAndSecretId(UUID projectId, UUID secretId);

    @Query(
            "select case when count(b) > 0 then true else false end from ProjectSecretBindingJpaEntity b"
                    + " where b.projectId = :projectId and lower(b.alias) = lower(:alias)")
    boolean existsByProjectIdAndAliasIgnoreCase(
            @Param("projectId") UUID projectId, @Param("alias") String alias);
}
