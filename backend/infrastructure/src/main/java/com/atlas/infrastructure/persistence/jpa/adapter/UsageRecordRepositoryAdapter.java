package com.atlas.infrastructure.persistence.jpa.adapter;

import com.atlas.application.port.out.UsageRecordRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.billing.UsageRecord;
import com.atlas.infrastructure.persistence.jpa.PageableFactory;
import com.atlas.infrastructure.persistence.jpa.entity.UsageRecordJpaEntity;
import com.atlas.infrastructure.persistence.jpa.repository.UsageRecordJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UsageRecordRepositoryAdapter implements UsageRecordRepositoryPort {

    private final UsageRecordJpaRepository repository;

    @Override
    public UsageRecord save(UsageRecord record) {
        return toDomain(repository.save(toEntity(record)));
    }

    @Override
    public PageResult<UsageRecord> search(PageQuery pageQuery) {
        Page<UsageRecordJpaEntity> page = repository.findAll(PageableFactory.from(pageQuery));
        List<UsageRecord> content = page.getContent().stream().map(this::toDomain).toList();
        return PageResult.of(content, page.getNumber(), page.getSize(), page.getTotalElements(), pageQuery.sort());
    }

    private UsageRecord toDomain(UsageRecordJpaEntity entity) {
        return UsageRecord.rehydrate(
                entity.getId(),
                entity.getMeter(),
                entity.getQuantity(),
                entity.getPeriodStart(),
                entity.getPeriodEnd(),
                entity.getDimensions(),
                entity.getCreatedAt());
    }

    private UsageRecordJpaEntity toEntity(UsageRecord domain) {
        UsageRecordJpaEntity entity = new UsageRecordJpaEntity();
        entity.setId(domain.getId());
        entity.setMeter(domain.getMeter());
        entity.setQuantity(domain.getQuantity());
        entity.setPeriodStart(domain.getPeriodStart());
        entity.setPeriodEnd(domain.getPeriodEnd());
        entity.setDimensions(domain.getDimensions());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
