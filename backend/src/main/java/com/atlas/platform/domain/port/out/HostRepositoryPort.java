package com.atlas.platform.domain.port.out;

import com.atlas.platform.domain.model.Host;
import com.atlas.platform.domain.model.PageResult;
import java.util.Optional;
import java.util.UUID;

public interface HostRepositoryPort {

    Optional<Host> findById(UUID installationId, UUID id);

    PageResult<Host> search(
            UUID installationId,
            String hostname,
            Boolean online,
            int page,
            int size,
            String sortBy,
            boolean ascending);

    long countByInstallation(UUID installationId);
}
