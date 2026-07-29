package com.atlas.infrastructure.persistence.jpa.adapter;

import com.atlas.application.port.out.PipelineRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.pipeline.Pipeline;
import com.atlas.infrastructure.persistence.jpa.PageableFactory;
import com.atlas.infrastructure.persistence.jpa.entity.PipelineJpaEntity;
import com.atlas.infrastructure.persistence.jpa.mapper.PipelineJpaMapper;
import com.atlas.infrastructure.persistence.jpa.repository.PipelineJpaRepository;
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
public class PipelineRepositoryAdapter implements PipelineRepositoryPort {

    private final PipelineJpaRepository repository;
    private final PipelineJpaMapper mapper;

    @Override
    public Pipeline save(Pipeline pipeline) {
        return mapper.toDomain(repository.save(mapper.toEntity(pipeline)));
    }

    @Override
    public Optional<Pipeline> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Pipeline> findByWebhookToken(String webhookToken) {
        return repository.findByWebhookToken(webhookToken).map(mapper::toDomain);
    }

    @Override
    public List<Pipeline> findByServiceId(UUID serviceId) {
        return repository.findByServiceIdOrderByCreatedAtAsc(serviceId).stream()
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
    public PageResult<Pipeline> search(UUID projectId, String name, PageQuery pageQuery) {
        Specification<PipelineJpaEntity> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (projectId != null) {
                predicates.add(cb.equal(root.get("projectId"), projectId));
            }
            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<PipelineJpaEntity> page = repository.findAll(specification, PageableFactory.from(pageQuery));
        List<Pipeline> content = page.getContent().stream().map(mapper::toDomain).toList();
        return PageResult.of(content, page.getNumber(), page.getSize(), page.getTotalElements(), pageQuery.sort());
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
