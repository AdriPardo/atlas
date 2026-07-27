package com.atlas.application.port.out;

import com.atlas.domain.observability.AlertEventType;
import com.atlas.domain.observability.AlertRule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRuleRepositoryPort {

    AlertRule save(AlertRule rule);

    Optional<AlertRule> findById(UUID id);

    List<AlertRule> findAll();

    List<AlertRule> findByEventType(AlertEventType eventType);

    void deleteById(UUID id);
}
