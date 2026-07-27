package com.atlas.application.port.out;

import com.atlas.domain.cron.CronJob;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CronJobRepositoryPort {

    CronJob save(CronJob cronJob);

    Optional<CronJob> findById(UUID id);

    List<CronJob> findAll();

    List<CronJob> findEnabled();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    void deleteById(UUID id);
}
