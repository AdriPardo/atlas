package com.atlas.infrastructure.persistence.jpa.adapter;

import com.atlas.application.port.out.CronJobRepositoryPort;
import com.atlas.domain.cron.CronJob;
import com.atlas.domain.cron.CronTargetType;
import com.atlas.infrastructure.persistence.jpa.entity.CronJobJpaEntity;
import com.atlas.infrastructure.persistence.jpa.repository.CronJobJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CronJobRepositoryAdapter implements CronJobRepositoryPort {

    private final CronJobJpaRepository repository;

    @Override
    public CronJob save(CronJob cronJob) {
        return toDomain(repository.save(toEntity(cronJob)));
    }

    @Override
    public Optional<CronJob> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<CronJob> findAll() {
        return repository.findAllByOrderByCreatedAtAsc().stream().map(this::toDomain).toList();
    }

    @Override
    public List<CronJob> findEnabled() {
        return repository.findByEnabledTrueOrderByCreatedAtAsc().stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByNameIgnoreCase(String name) {
        return repository.existsByNameIgnoreCase(name);
    }

    @Override
    public boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id) {
        return repository.existsByNameIgnoreCaseAndIdNot(name, id);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    private CronJob toDomain(CronJobJpaEntity entity) {
        return CronJob.rehydrate(
                entity.getId(),
                entity.getName(),
                entity.getCronExpression(),
                CronTargetType.valueOf(entity.getTargetType()),
                entity.getTargetId(),
                entity.isEnabled(),
                entity.getLastFiredAt(),
                entity.getLastError(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private CronJobJpaEntity toEntity(CronJob cronJob) {
        CronJobJpaEntity entity = new CronJobJpaEntity();
        entity.setId(cronJob.getId());
        entity.setName(cronJob.getName());
        entity.setCronExpression(cronJob.getCronExpression());
        entity.setTargetType(cronJob.getTargetType().name());
        entity.setTargetId(cronJob.getTargetId());
        entity.setEnabled(cronJob.isEnabled());
        entity.setLastFiredAt(cronJob.getLastFiredAt());
        entity.setLastError(cronJob.getLastError());
        entity.setCreatedAt(cronJob.getCreatedAt());
        entity.setUpdatedAt(cronJob.getUpdatedAt());
        return entity;
    }
}
