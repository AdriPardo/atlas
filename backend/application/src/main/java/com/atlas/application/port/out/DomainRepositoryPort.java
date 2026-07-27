package com.atlas.application.port.out;

import com.atlas.domain.networking.Domain;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DomainRepositoryPort {

    Domain save(Domain domain);

    Optional<Domain> findById(UUID id);

    List<Domain> findByProjectId(UUID projectId);

    boolean existsByProjectIdAndHostnameIgnoreCase(UUID projectId, String hostname);

    boolean existsByProjectIdAndHostnameIgnoreCaseAndIdNot(UUID projectId, String hostname, UUID id);

    void deleteById(UUID id);
}
