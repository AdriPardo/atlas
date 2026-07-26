package com.atlas.application.port.out;

import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.host.Host;
import java.util.Optional;
import java.util.UUID;

public interface HostRepositoryPort {

    Host save(Host host);

    Optional<Host> findById(UUID id);

    boolean existsByHostname(String hostname);

    boolean existsByHostnameAndIdNot(String hostname, UUID id);

    PageResult<Host> search(String hostname, Boolean online, PageQuery pageQuery);

    void deleteById(UUID id);

    long count();
}
