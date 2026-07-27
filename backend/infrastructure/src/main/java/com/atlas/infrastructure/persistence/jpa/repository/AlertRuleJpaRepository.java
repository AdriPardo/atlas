package com.atlas.infrastructure.persistence.jpa.repository;

import com.atlas.infrastructure.persistence.jpa.entity.AlertRuleJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRuleJpaRepository extends JpaRepository<AlertRuleJpaEntity, UUID> {

    List<AlertRuleJpaEntity> findAllByOrderByCreatedAtAsc();

    List<AlertRuleJpaEntity> findByEventTypeOrderByCreatedAtAsc(String eventType);
}
