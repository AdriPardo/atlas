package com.atlas.application.host;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.port.out.ContainerRuntimePort;
import com.atlas.domain.host.Host;
import com.atlas.domain.runtime.ContainerSnapshot;
import com.atlas.domain.shared.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListHostContainersUseCaseTest {

    @Mock
    private HostRuntimeSupport hostRuntimeSupport;

    @Mock
    private ContainerRuntimePort containerRuntime;

    private ListHostContainersUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListHostContainersUseCase(hostRuntimeSupport, containerRuntime);
    }

    @Test
    void listsContainersForHost() {
        UUID hostId = UUID.randomUUID();
        Host host = Host.create("edge-1", "10.0.0.1", "linux", "27.0", true, null, null, null, null);
        List<ContainerSnapshot> expected = List.of(
                new ContainerSnapshot("abc", "nginx", "nginx:latest", "running", "Up 2 hours", "80/tcp", ""));
        when(hostRuntimeSupport.requireHost(hostId)).thenReturn(host);
        when(hostRuntimeSupport.resolveSshKey(host)).thenReturn(Optional.empty());
        when(containerRuntime.listContainers(eq(host), eq(Optional.empty()))).thenReturn(expected);

        assertEquals(expected, useCase.execute(hostId));
        verify(containerRuntime).listContainers(host, Optional.empty());
    }

    @Test
    void failsWhenHostMissing() {
        UUID hostId = UUID.randomUUID();
        when(hostRuntimeSupport.requireHost(hostId)).thenThrow(new NotFoundException("Host not found"));
        assertThrows(NotFoundException.class, () -> useCase.execute(hostId));
    }
}
