package com.atlas.application.host;

import com.atlas.application.port.out.HostConnectorPort;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.domain.host.Host;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExecuteSyncHostJobUseCase {

    private final HostRepositoryPort hostRepository;
    private final HostConnectorPort hostConnector;

    @Transactional
    public Host execute(UUID hostId) {
        Host host = hostRepository
                .findById(hostId)
                .orElseThrow(() -> new NotFoundException("Host not found: " + hostId));

        HostConnectorPort.HostInspection inspection = hostConnector.inspect(host);
        String os = blankTo(inspection.operatingSystem(), host.getOperatingSystem());
        String docker = blankTo(inspection.dockerVersion(), host.getDockerVersion());
        host.applyInspection(os, docker, inspection.reachable());
        return hostRepository.save(host);
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
