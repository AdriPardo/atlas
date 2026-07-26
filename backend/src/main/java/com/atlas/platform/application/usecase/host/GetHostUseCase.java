package com.atlas.platform.application.usecase.host;

import com.atlas.platform.application.security.InstallationContext;
import com.atlas.platform.domain.exception.NotFoundException;
import com.atlas.platform.domain.model.Host;
import com.atlas.platform.domain.port.out.HostRepositoryPort;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetHostUseCase {

    private final HostRepositoryPort hostRepository;

    public GetHostUseCase(HostRepositoryPort hostRepository) {
        this.hostRepository = hostRepository;
    }

    @Transactional(readOnly = true)
    public Host execute(UUID id) {
        return hostRepository
                .findById(InstallationContext.currentInstallationId(), id)
                .orElseThrow(() -> new NotFoundException("Host not found: " + id));
    }
}
