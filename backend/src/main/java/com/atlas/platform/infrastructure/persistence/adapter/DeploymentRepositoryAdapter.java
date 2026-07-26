package com.atlas.platform.infrastructure.persistence.adapter;

import com.atlas.platform.domain.model.Deployment;
import com.atlas.platform.domain.model.DeploymentStatus;
import com.atlas.platform.domain.model.PageResult;
import com.atlas.platform.domain.port.out.DeploymentRepositoryPort;
import com.atlas.platform.infrastructure.persistence.entity.DeploymentJpaEntity;
import com.atlas.platform.infrastructure.persistence.mapper.DeploymentPersistenceMapper;
import com.atlas.platform.infrastructure.persistence.repository.DeploymentJpaRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class DeploymentRepositoryAdapter implements DeploymentRepositoryPort {

    private static final Set<String> SORTABLE = Set.of("startedAt", "finishedAt", "status");

    private final DeploymentJpaRepository repository;
    private final DeploymentPersistenceMapper mapper;

    public DeploymentRepositoryAdapter(
            DeploymentJpaRepository repository, DeploymentPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Deployment> findById(UUID installationId, UUID id) {
        return repository.findByInstallationIdAndId(installationId, id).map(mapper::toDomain);
    }

    @Override
    public PageResult<Deployment> search(
            UUID installationId,
            UUID applicationId,
            UUID hostId,
            DeploymentStatus status,
            int page,
            int size,
            String sortBy,
            boolean ascending) {
        Specification<DeploymentJpaEntity> spec = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("installationId"), installationId));
            if (applicationId != null) {
                predicates.add(cb.equal(root.get("applicationId"), applicationId));
            }
            if (hostId != null) {
                predicates.add(cb.equal(root.get("hostId"), hostId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        String property = SORTABLE.contains(sortBy) ? sortBy : "startedAt";
        var pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, property));
        var result = repository.findAll(spec, pageable);
        return new PageResult<>(
                result.getContent().stream().map(mapper::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }
}
