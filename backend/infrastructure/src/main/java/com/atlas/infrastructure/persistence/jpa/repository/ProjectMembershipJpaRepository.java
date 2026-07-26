package com.atlas.infrastructure.persistence.jpa.repository;

import com.atlas.infrastructure.persistence.jpa.entity.ProjectMembershipJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProjectMembershipJpaRepository extends JpaRepository<ProjectMembershipJpaEntity, UUID> {

    Optional<ProjectMembershipJpaEntity> findByProjectIdAndUserId(UUID projectId, UUID userId);

    List<ProjectMembershipJpaEntity> findByProjectIdOrderByCreatedAtAsc(UUID projectId);

    @Query("select m.projectId from ProjectMembershipJpaEntity m where m.userId = :userId")
    List<UUID> findProjectIdsByUserId(UUID userId);
}
