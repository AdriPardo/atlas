package com.atlas.infrastructure.persistence.jpa.repository;

import com.atlas.infrastructure.persistence.jpa.entity.SecretJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SecretJpaRepository extends JpaRepository<SecretJpaEntity, UUID> {

    @Query("select s from SecretJpaEntity s where s.projectId is null and lower(s.name) = lower(:name)")
    Optional<SecretJpaEntity> findGlobalByNameIgnoreCase(@Param("name") String name);

    @Query(
            "select case when count(s) > 0 then true else false end from SecretJpaEntity s"
                    + " where s.projectId is null and lower(s.name) = lower(:name)")
    boolean existsGlobalByNameIgnoreCase(@Param("name") String name);

    @Query(
            "select s from SecretJpaEntity s where s.projectId = :projectId and lower(s.name) = lower(:name)")
    Optional<SecretJpaEntity> findByProjectIdAndNameIgnoreCase(
            @Param("projectId") UUID projectId, @Param("name") String name);

    @Query(
            "select case when count(s) > 0 then true else false end from SecretJpaEntity s"
                    + " where s.projectId = :projectId and lower(s.name) = lower(:name)")
    boolean existsByProjectIdAndNameIgnoreCase(
            @Param("projectId") UUID projectId, @Param("name") String name);

    @Query("select s from SecretJpaEntity s where s.projectId is null order by s.name asc")
    List<SecretJpaEntity> findAllGlobal();

    List<SecretJpaEntity> findByProjectIdOrderByNameAsc(UUID projectId);
}
