package com.atlas.infrastructure.persistence.jpa.repository;

import com.atlas.infrastructure.persistence.jpa.entity.CronJobJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CronJobJpaRepository extends JpaRepository<CronJobJpaEntity, UUID> {

    List<CronJobJpaEntity> findAllByOrderByCreatedAtAsc();

    List<CronJobJpaEntity> findByEnabledTrueOrderByCreatedAtAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
