package com.atlas.infrastructure.persistence.jpa.repository;

import com.atlas.infrastructure.persistence.jpa.entity.DeploymentJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeploymentJpaRepository
        extends JpaRepository<DeploymentJpaEntity, UUID>, JpaSpecificationExecutor<DeploymentJpaEntity> {

    boolean existsByServiceId(UUID serviceId);

    boolean existsByHostId(UUID hostId);

    @Query(
            """
            select case when count(d) > 0 then true else false end
            from DeploymentJpaEntity d
            where d.serviceId in (
                select s.id from ServiceJpaEntity s where s.projectId = :projectId
            )
            """)
    boolean existsByProjectId(@Param("projectId") UUID projectId);
}
