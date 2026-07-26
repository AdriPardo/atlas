package com.atlas.platform.domain.port.out;

import com.atlas.platform.domain.model.Application;
import com.atlas.platform.domain.model.ApplicationStatus;
import com.atlas.platform.domain.model.PageResult;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepositoryPort {

    Application save(Application application);

    Optional<Application> findById(UUID installationId, UUID id);

    PageResult<Application> search(
            UUID installationId,
            String name,
            ApplicationStatus status,
            int page,
            int size,
            String sortBy,
            boolean ascending);

    boolean existsByName(UUID installationId, String name, UUID excludingId);

    void delete(Application application);
}
