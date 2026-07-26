package com.atlas.infrastructure.persistence.jpa.adapter;

import com.atlas.application.port.out.AuditRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.audit.AuditEntry;
import com.atlas.infrastructure.persistence.jpa.PageableFactory;
import com.atlas.infrastructure.persistence.jpa.entity.AuditEntryJpaEntity;
import com.atlas.infrastructure.persistence.jpa.repository.AuditEntryJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditRepositoryAdapter implements AuditRepositoryPort {

    private final AuditEntryJpaRepository repository;

    @Override
    public AuditEntry save(AuditEntry entry) {
        return toDomain(repository.save(toEntity(entry)));
    }

    @Override
    public PageResult<AuditEntry> search(PageQuery pageQuery) {
        Page<AuditEntryJpaEntity> page = repository.findAll(PageableFactory.from(pageQuery));
        List<AuditEntry> content = page.getContent().stream().map(this::toDomain).toList();
        return PageResult.of(content, page.getNumber(), page.getSize(), page.getTotalElements(), pageQuery.sort());
    }

    private AuditEntry toDomain(AuditEntryJpaEntity entity) {
        return AuditEntry.rehydrate(
                entity.getId(),
                entity.getActorUserId(),
                entity.getActorUsername(),
                entity.getAction(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getMetadata(),
                entity.getCreatedAt());
    }

    private AuditEntryJpaEntity toEntity(AuditEntry domain) {
        AuditEntryJpaEntity entity = new AuditEntryJpaEntity();
        entity.setId(domain.getId());
        entity.setActorUserId(domain.getActorUserId());
        entity.setActorUsername(domain.getActorUsername());
        entity.setAction(domain.getAction());
        entity.setResourceType(domain.getResourceType());
        entity.setResourceId(domain.getResourceId());
        entity.setMetadata(domain.getMetadata());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
