package com.atlas.infrastructure.persistence.jpa.adapter;

import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.host.Host;
import com.atlas.infrastructure.persistence.jpa.PageableFactory;
import com.atlas.infrastructure.persistence.jpa.entity.HostJpaEntity;
import com.atlas.infrastructure.persistence.jpa.mapper.HostJpaMapper;
import com.atlas.infrastructure.persistence.jpa.repository.HostJpaRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HostRepositoryAdapter implements HostRepositoryPort {

    private final HostJpaRepository repository;
    private final HostJpaMapper mapper;

    @Override
    public Host save(Host host) {
        return mapper.toDomain(repository.save(mapper.toEntity(host)));
    }

    @Override
    public Optional<Host> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Host> findByHostnameIgnoreCase(String hostname) {
        return repository.findByHostnameIgnoreCase(hostname).map(mapper::toDomain);
    }

    @Override
    public boolean existsByHostname(String hostname) {
        return repository.existsByHostnameIgnoreCase(hostname);
    }

    @Override
    public List<Host> listForPlacement() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "createdAt")).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByHostnameAndIdNot(String hostname, UUID id) {
        return repository.existsByHostnameIgnoreCaseAndIdNot(hostname, id);
    }

    @Override
    public PageResult<Host> search(String hostname, Boolean online, PageQuery pageQuery) {
        Specification<HostJpaEntity> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hostname != null && !hostname.isBlank()) {
                predicates.add(
                        cb.like(cb.lower(root.get("hostname")), "%" + hostname.toLowerCase() + "%"));
            }
            if (online != null) {
                predicates.add(cb.equal(root.get("online"), online));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<HostJpaEntity> page = repository.findAll(specification, PageableFactory.from(pageQuery));
        List<Host> content = page.getContent().stream().map(mapper::toDomain).toList();
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
