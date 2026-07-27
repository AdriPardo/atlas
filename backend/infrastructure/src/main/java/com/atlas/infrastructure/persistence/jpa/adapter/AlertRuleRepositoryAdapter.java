package com.atlas.infrastructure.persistence.jpa.adapter;

import com.atlas.application.port.out.AlertRuleRepositoryPort;
import com.atlas.domain.observability.AlertEventType;
import com.atlas.domain.observability.AlertRule;
import com.atlas.domain.observability.AlertRuleStatus;
import com.atlas.infrastructure.persistence.jpa.entity.AlertRuleJpaEntity;
import com.atlas.infrastructure.persistence.jpa.repository.AlertRuleJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertRuleRepositoryAdapter implements AlertRuleRepositoryPort {

    private final AlertRuleJpaRepository repository;

    @Override
    public AlertRule save(AlertRule rule) {
        return toDomain(repository.save(toEntity(rule)));
    }

    @Override
    public Optional<AlertRule> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<AlertRule> findAll() {
        return repository.findAllByOrderByCreatedAtAsc().stream().map(this::toDomain).toList();
    }

    @Override
    public List<AlertRule> findByEventType(AlertEventType eventType) {
        return repository.findByEventTypeOrderByCreatedAtAsc(eventType.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    private AlertRule toDomain(AlertRuleJpaEntity entity) {
        return AlertRule.rehydrate(
                entity.getId(),
                entity.getName(),
                AlertEventType.valueOf(entity.getEventType()),
                entity.getProjectId(),
                entity.getChannelId(),
                AlertRuleStatus.valueOf(entity.getStatus()),
                entity.getLastFiredAt(),
                entity.getLastError(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private AlertRuleJpaEntity toEntity(AlertRule rule) {
        AlertRuleJpaEntity entity = new AlertRuleJpaEntity();
        entity.setId(rule.getId());
        entity.setName(rule.getName());
        entity.setEventType(rule.getEventType().name());
        entity.setProjectId(rule.getProjectId());
        entity.setChannelId(rule.getChannelId());
        entity.setStatus(rule.getStatus().name());
        entity.setLastFiredAt(rule.getLastFiredAt());
        entity.setLastError(rule.getLastError());
        entity.setCreatedAt(rule.getCreatedAt());
        entity.setUpdatedAt(rule.getUpdatedAt());
        return entity;
    }
}
