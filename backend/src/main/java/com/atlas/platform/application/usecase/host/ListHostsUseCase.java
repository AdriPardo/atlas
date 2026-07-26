package com.atlas.platform.application.usecase.host;

import com.atlas.platform.application.security.InstallationContext;
import com.atlas.platform.domain.model.Host;
import com.atlas.platform.domain.model.PageResult;
import com.atlas.platform.domain.port.out.HostRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListHostsUseCase {

    private final HostRepositoryPort hostRepository;

    public ListHostsUseCase(HostRepositoryPort hostRepository) {
        this.hostRepository = hostRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<Host> execute(
            String hostname, Boolean online, int page, int size, String sortBy, boolean ascending) {
        return hostRepository.search(
                InstallationContext.currentInstallationId(),
                hostname,
                online,
                page,
                size,
                sortBy,
                ascending);
    }
}
