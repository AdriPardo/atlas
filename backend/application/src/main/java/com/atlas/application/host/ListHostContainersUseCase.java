package com.atlas.application.host;

import com.atlas.application.port.out.ContainerRuntimePort;
import com.atlas.domain.host.Host;
import com.atlas.domain.runtime.ContainerSnapshot;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListHostContainersUseCase {

    private final HostRuntimeSupport hostRuntimeSupport;
    private final ContainerRuntimePort containerRuntime;

    @Transactional(readOnly = true)
    public List<ContainerSnapshot> execute(UUID hostId) {
        Host host = hostRuntimeSupport.requireHost(hostId);
        return containerRuntime.listContainers(host, hostRuntimeSupport.resolveSshKey(host));
    }
}
