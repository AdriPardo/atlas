package com.atlas.infrastructure.persistence.jpa.adapter;

import com.atlas.application.port.out.ApplicationRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.application.Application;
import com.atlas.domain.application.ApplicationStatus;
import com.atlas.infrastructure.persistence.jpa.PageableFactory;
import com.atlas.infrastructure.persistence.jpa.entity.ApplicationJpaEntity;
import com.atlas.infrastructure.persistence.jpa.mapper.ApplicationJpaMapper;
import com.atlas.infrastructure.persistence.jpa.repository.ApplicationJpaRepository;
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
public class ApplicationRepositoryAdapter implements ApplicationRepositoryPort {

    private final ApplicationJpaRepository repository;
    private final ApplicationJpaMapper mapper;

    @Override
    public Application save(Application application) {
        return mapper.toDomain(repository.save(mapper.toEntity(application)));
    }

    @Override
    public Optional<Application> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return repository.existsByNameIgnoreCase(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, UUID id) {
        return repository.existsByNameIgnoreCaseAndIdNot(name, id);
    }

    @Override
    public PageResult<Application> search(String name, ApplicationStatus status, PageQuery pageQuery) {
        Specification<ApplicationJpaEntity> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status.name()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<ApplicationJpaEntity> page = repository.findAll(specification, PageableFactory.from(pageQuery));
        List<Application> content = page.getContent().stream().map(mapper::toDomain).toList();
        return PageResult.of(content, page.getNumber(), page.getSize(), page.getTotalElements(), pageQuery.sort());
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public long count() {
        return repository.count();
    }
}
