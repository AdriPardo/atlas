package com.atlas.infrastructure.persistence.jpa.adapter;

import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.deployment.DeploymentStatus;
import com.atlas.infrastructure.persistence.jpa.PageableFactory;
import com.atlas.infrastructure.persistence.jpa.entity.DeploymentJpaEntity;
import com.atlas.infrastructure.persistence.jpa.mapper.DeploymentJpaMapper;
import com.atlas.infrastructure.persistence.jpa.repository.DeploymentJpaRepository;
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
public class DeploymentRepositoryAdapter implements DeploymentRepositoryPort {

    private final DeploymentJpaRepository repository;
    private final DeploymentJpaMapper mapper;

    @Override
    public Deployment save(Deployment deployment) {
        return mapper.toDomain(repository.save(mapper.toEntity(deployment)));
    }

    @Override
    public Optional<Deployment> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public PageResult<Deployment> search(
            UUID serviceId, UUID hostId, DeploymentStatus status, PageQuery pageQuery) {
        Specification<DeploymentJpaEntity> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (serviceId != null) {
                predicates.add(cb.equal(root.get("serviceId"), serviceId));
            }
            if (hostId != null) {
                predicates.add(cb.equal(root.get("hostId"), hostId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status.name()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<DeploymentJpaEntity> page = repository.findAll(specification, PageableFactory.from(pageQuery));
        List<Deployment> content = page.getContent().stream().map(mapper::toDomain).toList();
        return PageResult.of(content, page.getNumber(), page.getSize(), page.getTotalElements(), pageQuery.sort());
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsByServiceId(UUID serviceId) {
        return repository.existsByServiceId(serviceId);
    }

    @Override
    public boolean existsByProjectId(UUID projectId) {
        return repository.existsByProjectId(projectId);
    }

    @Override
    public boolean existsByHostId(UUID hostId) {
        return repository.existsByHostId(hostId);
    }

    @Override
    public long count() {
        return repository.count();
    }
}
