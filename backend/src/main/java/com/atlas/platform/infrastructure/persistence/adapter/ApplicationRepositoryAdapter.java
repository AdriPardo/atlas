package com.atlas.platform.infrastructure.persistence.adapter;

import com.atlas.platform.domain.model.Application;
import com.atlas.platform.domain.model.ApplicationStatus;
import com.atlas.platform.domain.model.PageResult;
import com.atlas.platform.domain.port.out.ApplicationRepositoryPort;
import com.atlas.platform.infrastructure.persistence.entity.ApplicationJpaEntity;
import com.atlas.platform.infrastructure.persistence.mapper.ApplicationPersistenceMapper;
import com.atlas.platform.infrastructure.persistence.repository.ApplicationJpaRepository;
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
public class ApplicationRepositoryAdapter implements ApplicationRepositoryPort {

    private static final Set<String> SORTABLE =
            Set.of("name", "status", "createdAt", "updatedAt", "domain");

    private final ApplicationJpaRepository repository;
    private final ApplicationPersistenceMapper mapper;

    public ApplicationRepositoryAdapter(
            ApplicationJpaRepository repository, ApplicationPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Application save(Application application) {
        return mapper.toDomain(repository.save(mapper.toEntity(application)));
    }

    @Override
    public Optional<Application> findById(UUID installationId, UUID id) {
        return repository.findByInstallationIdAndId(installationId, id).map(mapper::toDomain);
    }

    @Override
    public PageResult<Application> search(
            UUID installationId,
            String name,
            ApplicationStatus status,
            int page,
            int size,
            String sortBy,
            boolean ascending) {
        Specification<ApplicationJpaEntity> spec = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("installationId"), installationId));
            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
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
    public boolean existsByName(UUID installationId, String name, UUID excludingId) {
        if (excludingId == null) {
            return repository.existsByInstallationIdAndNameIgnoreCase(installationId, name);
        }
        return repository.existsByInstallationIdAndNameIgnoreCaseAndIdNot(
                installationId, name, excludingId);
    }

    @Override
    public void delete(Application application) {
        repository.deleteById(application.getId());
    }
}
