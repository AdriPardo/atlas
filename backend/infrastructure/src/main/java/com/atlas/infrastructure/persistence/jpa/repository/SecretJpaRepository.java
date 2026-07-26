package com.atlas.infrastructure.persistence.jpa.repository;

import com.atlas.infrastructure.persistence.jpa.entity.SecretJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecretJpaRepository extends JpaRepository<SecretJpaEntity, UUID> {

    Optional<SecretJpaEntity> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
