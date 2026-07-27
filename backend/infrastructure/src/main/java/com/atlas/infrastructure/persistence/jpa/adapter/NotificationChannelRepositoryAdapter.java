package com.atlas.infrastructure.persistence.jpa.adapter;

import com.atlas.application.port.out.NotificationChannelRepositoryPort;
import com.atlas.domain.observability.NotificationChannel;
import com.atlas.domain.observability.NotificationChannelType;
import com.atlas.infrastructure.persistence.jpa.entity.NotificationChannelJpaEntity;
import com.atlas.infrastructure.persistence.jpa.repository.NotificationChannelJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationChannelRepositoryAdapter implements NotificationChannelRepositoryPort {

    private final NotificationChannelJpaRepository repository;

    @Override
    public NotificationChannel save(NotificationChannel channel) {
        return toDomain(repository.save(toEntity(channel)));
    }

    @Override
    public Optional<NotificationChannel> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<NotificationChannel> findAll() {
        return repository.findAllByOrderByCreatedAtAsc().stream().map(this::toDomain).toList();
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

    private NotificationChannel toDomain(NotificationChannelJpaEntity entity) {
        return NotificationChannel.rehydrate(
                entity.getId(),
                entity.getName(),
                NotificationChannelType.valueOf(entity.getType()),
                entity.getTarget(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private NotificationChannelJpaEntity toEntity(NotificationChannel channel) {
        NotificationChannelJpaEntity entity = new NotificationChannelJpaEntity();
        entity.setId(channel.getId());
        entity.setName(channel.getName());
        entity.setType(channel.getType().name());
        entity.setTarget(channel.getTarget());
        entity.setEnabled(channel.isEnabled());
        entity.setCreatedAt(channel.getCreatedAt());
        entity.setUpdatedAt(channel.getUpdatedAt());
        return entity;
    }
}
