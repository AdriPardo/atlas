package com.atlas.application.host;

import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.application.secret.ResolveSecretValueUseCase;
import com.atlas.domain.host.ConnectionType;
import com.atlas.domain.host.Host;
import com.atlas.domain.shared.DomainException;
import com.atlas.domain.shared.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HostRuntimeSupport {

    private static final Pattern SAFE_CONTAINER_REF = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.\\/-]{0,127}");

    private final HostRepositoryPort hostRepository;
    private final ResolveSecretValueUseCase resolveSecretValue;

    public Host requireHost(UUID hostId) {
        return hostRepository
                .findById(hostId)
                .orElseThrow(() -> new NotFoundException("Host not found: " + hostId));
    }

    public Optional<String> resolveSshKey(Host host) {
        if (host.getConnectionType() != ConnectionType.SSH) {
            return Optional.empty();
        }
        if (host.getSshPrivateKeySecretId() == null) {
            throw new DomainException("SSH host requires sshPrivateKeySecretId for runtime operations");
        }
        return Optional.of(resolveSecretValue.byId(host.getSshPrivateKeySecretId()));
    }

    public String requireSafeContainerRef(String containerRef) {
        if (containerRef == null || !SAFE_CONTAINER_REF.matcher(containerRef).matches()) {
            throw new DomainException("Invalid container reference");
        }
        return containerRef;
    }
}
