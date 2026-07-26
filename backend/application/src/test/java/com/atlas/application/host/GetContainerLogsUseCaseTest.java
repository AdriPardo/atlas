package com.atlas.application.host;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.atlas.application.port.out.ContainerRuntimePort;
import com.atlas.domain.host.Host;
import com.atlas.domain.shared.DomainException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetContainerLogsUseCaseTest {

    @Mock
    private HostRuntimeSupport hostRuntimeSupport;

    @Mock
    private ContainerRuntimePort containerRuntime;

    @Test
    void rejectsUnsafeContainerRef() {
        GetContainerLogsUseCase useCase = new GetContainerLogsUseCase(hostRuntimeSupport, containerRuntime);
        UUID hostId = UUID.randomUUID();
        Host host = Host.create("edge-1", "10.0.0.1", "linux", "27.0", true, null, null, null, null);
        when(hostRuntimeSupport.requireHost(hostId)).thenReturn(host);
        when(hostRuntimeSupport.requireSafeContainerRef("bad;rm")).thenThrow(new DomainException("Invalid"));

        assertThrows(DomainException.class, () -> useCase.execute(hostId, "bad;rm", 100));
    }

    @Test
    void fetchesLogsWithNormalizedTail() {
        GetContainerLogsUseCase useCase = new GetContainerLogsUseCase(hostRuntimeSupport, containerRuntime);
        UUID hostId = UUID.randomUUID();
        Host host = Host.create("edge-1", "10.0.0.1", "linux", "27.0", true, null, null, null, null);
        when(hostRuntimeSupport.requireHost(hostId)).thenReturn(host);
        when(hostRuntimeSupport.requireSafeContainerRef("nginx")).thenReturn("nginx");
        when(hostRuntimeSupport.resolveSshKey(host)).thenReturn(Optional.empty());
        when(containerRuntime.containerLogs(eq(host), eq("nginx"), eq(200), any())).thenReturn("log-line");

        assertEquals("log-line", useCase.execute(hostId, "nginx", null));
    }
}
