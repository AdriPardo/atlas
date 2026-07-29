package com.atlas.infrastructure.persistence.jpa.repository;

import com.atlas.infrastructure.persistence.jpa.entity.UsageRecordJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageRecordJpaRepository extends JpaRepository<UsageRecordJpaEntity, UUID> {}
