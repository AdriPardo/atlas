package com.atlas.application.deployment;

import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.domain.host.ConnectionType;
import com.atlas.domain.host.Host;
import com.atlas.domain.shared.NotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autopilot host resolution (ADR-0010). Prefer shared LOCAL Docker hosts; seed a default if none exist.
 * Proxmox VM provisioning is a later slice.
 */
@Service
@RequiredArgsConstructor
public class AutopilotPlacementService {

    public static final String DEFAULT_LOCAL_HOSTNAME = "atlas-local";

    private final HostRepositoryPort hostRepository;

    @Transactional
    public Host resolveHost(UUID explicitHostId) {
        if (explicitHostId != null) {
            return hostRepository
                    .findById(explicitHostId)
                    .orElseThrow(() -> new NotFoundException("Host not found: " + explicitHostId));
        }
        List<Host> hosts = hostRepository.listForPlacement();
        if (hosts.isEmpty()) {
            return ensureDefaultLocalHost();
        }
        return hosts.stream().max(Comparator.comparingInt(AutopilotPlacementService::score)).orElseGet(this::ensureDefaultLocalHost);
    }

    private Host ensureDefaultLocalHost() {
        return hostRepository
                .findByHostnameIgnoreCase(DEFAULT_LOCAL_HOSTNAME)
                .orElseGet(() -> hostRepository.save(Host.create(
                        DEFAULT_LOCAL_HOSTNAME,
                        "127.0.0.1",
                        "linux",
                        "",
                        true,
                        ConnectionType.LOCAL,
                        null,
                        22,
                        null)));
    }

    static int score(Host host) {
        int score = 0;
        if (host.getConnectionType() == ConnectionType.LOCAL) {
            score += 100;
        }
        if (host.isOnline()) {
            score += 50;
        }
        String hostname = host.getHostname() == null ? "" : host.getHostname().toLowerCase(Locale.ROOT);
        if (DEFAULT_LOCAL_HOSTNAME.equals(hostname) || "default".equals(hostname)) {
            score += 25;
        }
        return score;
    }
}
