package com.atlas.application.host;

import com.atlas.application.port.out.ContainerRuntimePort;
import com.atlas.domain.host.Host;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetContainerLogsUseCase {

    private static final int DEFAULT_TAIL = 200;
    private static final int MAX_TAIL = 2000;

    private final HostRuntimeSupport hostRuntimeSupport;
    private final ContainerRuntimePort containerRuntime;

    @Transactional(readOnly = true)
    public String execute(UUID hostId, String containerRef, Integer tail) {
        Host host = hostRuntimeSupport.requireHost(hostId);
        String safeRef = hostRuntimeSupport.requireSafeContainerRef(containerRef);
        int lines = normalizeTail(tail);
        return containerRuntime.containerLogs(host, safeRef, lines, hostRuntimeSupport.resolveSshKey(host));
    }

    private static int normalizeTail(Integer tail) {
        if (tail == null || tail <= 0) {
            return DEFAULT_TAIL;
        }
        return Math.min(tail, MAX_TAIL);
    }
}
