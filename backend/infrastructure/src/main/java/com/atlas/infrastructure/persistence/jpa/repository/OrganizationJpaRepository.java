package com.atlas.infrastructure.persistence.jpa.repository;

import com.atlas.infrastructure.persistence.jpa.entity.OrganizationJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationJpaRepository extends JpaRepository<OrganizationJpaEntity, UUID> {}
