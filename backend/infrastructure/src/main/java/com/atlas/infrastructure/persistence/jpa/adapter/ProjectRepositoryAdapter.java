package com.atlas.infrastructure.persistence.jpa.adapter;

import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.project.Project;
import com.atlas.domain.project.ProjectStatus;
import com.atlas.infrastructure.persistence.jpa.PageableFactory;
import com.atlas.infrastructure.persistence.jpa.entity.ProjectJpaEntity;
import com.atlas.infrastructure.persistence.jpa.mapper.ProjectJpaMapper;
import com.atlas.infrastructure.persistence.jpa.repository.ProjectJpaRepository;
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
public class ProjectRepositoryAdapter implements ProjectRepositoryPort {

    private final ProjectJpaRepository repository;
    private final ProjectJpaMapper mapper;

    @Override
    public Project save(Project project) {
        return mapper.toDomain(repository.save(mapper.toEntity(project)));
    }

    @Override
    public Optional<Project> findById(UUID id) {
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
    public boolean existsBySlug(String slug) {
        return repository.existsBySlugIgnoreCase(slug);
    }

    @Override
    public boolean existsBySlugAndIdNot(String slug, UUID id) {
        return repository.existsBySlugIgnoreCaseAndIdNot(slug, id);
    }

    @Override
    public PageResult<Project> search(String name, ProjectStatus status, PageQuery pageQuery) {
        Specification<ProjectJpaEntity> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status.name()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<ProjectJpaEntity> page = repository.findAll(specification, PageableFactory.from(pageQuery));
        List<Project> content = page.getContent().stream().map(mapper::toDomain).toList();
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
