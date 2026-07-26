package com.atlas.application.host;

import com.atlas.application.port.out.ContainerRuntimePort;
import com.atlas.domain.host.Host;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestartContainerUseCase {

    private final HostRuntimeSupport hostRuntimeSupport;
    private final ContainerRuntimePort containerRuntime;

    @Transactional(readOnly = true)
    public void execute(UUID hostId, String containerRef) {
        Host host = hostRuntimeSupport.requireHost(hostId);
        String safeRef = hostRuntimeSupport.requireSafeContainerRef(containerRef);
        containerRuntime.restartContainer(
                host, safeRef, hostRuntimeSupport.resolveSshKey(host), line -> {});
    }
}
