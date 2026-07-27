package com.atlas.application.port.out;

import com.atlas.domain.observability.NotificationChannel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationChannelRepositoryPort {

    NotificationChannel save(NotificationChannel channel);

    Optional<NotificationChannel> findById(UUID id);

    List<NotificationChannel> findAll();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    void deleteById(UUID id);
}
