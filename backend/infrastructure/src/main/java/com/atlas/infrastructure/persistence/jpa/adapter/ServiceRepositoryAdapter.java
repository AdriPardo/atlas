package com.atlas.infrastructure.persistence.jpa.adapter;

import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.service.ServiceStatus;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.infrastructure.persistence.jpa.PageableFactory;
import com.atlas.infrastructure.persistence.jpa.entity.ServiceJpaEntity;
import com.atlas.infrastructure.persistence.jpa.mapper.ServiceJpaMapper;
import com.atlas.infrastructure.persistence.jpa.repository.ServiceJpaRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceRepositoryAdapter implements ServiceRepositoryPort {

    private final ServiceJpaRepository repository;
    private final ServiceJpaMapper mapper;

    @Override
    public ServiceUnit save(ServiceUnit service) {
        return mapper.toDomain(repository.save(mapper.toEntity(service)));
    }

    @Override
    public Optional<ServiceUnit> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ServiceUnit> findDefaultByProjectId(UUID projectId) {
        return repository
                .findFirstByProjectIdAndNameIgnoreCase(projectId, ServiceUnit.DEFAULT_NAME)
                .or(() -> repository.findFirstByProjectIdOrderByCreatedAtAsc(projectId))
                .map(mapper::toDomain);
    }

    @Override
    public List<ServiceUnit> findByProjectId(UUID projectId) {
        return repository.findByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByProjectIdAndName(UUID projectId, String name) {
        return repository.existsByProjectIdAndNameIgnoreCase(projectId, name);
    }

    @Override
    public boolean existsByProjectIdAndNameAndIdNot(UUID projectId, String name, UUID id) {
        return repository.existsByProjectIdAndNameIgnoreCaseAndIdNot(projectId, name, id);
    }

    @Override
    public PageResult<ServiceUnit> search(
            UUID projectId, String name, ServiceStatus status, PageQuery pageQuery) {
        Specification<ServiceJpaEntity> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (projectId != null) {
                predicates.add(cb.equal(root.get("projectId"), projectId));
            }
            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status.name()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<ServiceJpaEntity> page = repository.findAll(specification, PageableFactory.from(pageQuery));
        List<ServiceUnit> content = page.getContent().stream().map(mapper::toDomain).toList();
        return PageResult.of(content, page.getNumber(), page.getSize(), page.getTotalElements(), pageQuery.sort());
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsByProjectId(UUID projectId) {
        return repository.existsByProjectId(projectId);
    }

    @Override
    public long count() {
        return repository.count();
    }
}
