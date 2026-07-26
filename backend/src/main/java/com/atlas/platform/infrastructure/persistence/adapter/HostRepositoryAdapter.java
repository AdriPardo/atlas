package com.atlas.platform.infrastructure.persistence.adapter;

import com.atlas.platform.domain.model.Host;
import com.atlas.platform.domain.model.PageResult;
import com.atlas.platform.domain.port.out.HostRepositoryPort;
import com.atlas.platform.infrastructure.persistence.entity.HostJpaEntity;
import com.atlas.platform.infrastructure.persistence.mapper.HostPersistenceMapper;
import com.atlas.platform.infrastructure.persistence.repository.HostJpaRepository;
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
public class HostRepositoryAdapter implements HostRepositoryPort {

    private static final Set<String> SORTABLE = Set.of("hostname", "ip", "online", "createdAt");

    private final HostJpaRepository repository;
    private final HostPersistenceMapper mapper;

    public HostRepositoryAdapter(HostJpaRepository repository, HostPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Host> findById(UUID installationId, UUID id) {
        return repository.findByInstallationIdAndId(installationId, id).map(mapper::toDomain);
    }

    @Override
    public PageResult<Host> search(
            UUID installationId,
            String hostname,
            Boolean online,
            int page,
            int size,
            String sortBy,
            boolean ascending) {
        Specification<HostJpaEntity> spec = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("installationId"), installationId));
            if (hostname != null && !hostname.isBlank()) {
                predicates.add(
                        cb.like(cb.lower(root.get("hostname")), "%" + hostname.toLowerCase() + "%"));
            }
            if (online != null) {
                predicates.add(cb.equal(root.get("online"), online));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        String property = SORTABLE.contains(sortBy) ? sortBy : "createdAt";
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

    @Override
    public long countByInstallation(UUID installationId) {
        return repository.countByInstallationId(installationId);
    }
}
