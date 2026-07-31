package com.atlas.infrastructure.adapter.runtime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.atlas.application.port.out.ContainerRuntimePort;
import com.atlas.application.port.out.RuntimeOrchestratorPort.RuntimeApplyCommand;
import com.atlas.application.port.out.RuntimeOrchestratorPort.RuntimeTeardownCommand;
import com.atlas.domain.host.ConnectionType;
import com.atlas.domain.host.Host;
import com.atlas.domain.runtime.RuntimeCapability;
import com.atlas.domain.shared.DomainException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RoutingRuntimeOrchestratorAdapterTest {

    @TempDir
    Path workspace;

    @Test
    void routesComposeToComposeAdapter() {
        ContainerRuntimePort containerRuntime = mock(ContainerRuntimePort.class);
        RoutingRuntimeOrchestratorAdapter router = new RoutingRuntimeOrchestratorAdapter(containerRuntime);
        Host host = Host.create("local", "127.0.0.1", "linux", "26", true, ConnectionType.LOCAL, null, 22, null);

        router.apply(new RuntimeApplyCommand(
                host, workspace, RuntimeCapability.COMPOSE, "docker-compose.yml", Optional.empty(), line -> {}));

        verify(containerRuntime).composeUp(any(), any(), any(), any(), any());
        verifyNoInteractionsPodman(containerRuntime);
    }

    @Test
    void routesPodmanToPodmanAdapter() {
        ContainerRuntimePort containerRuntime = mock(ContainerRuntimePort.class);
        RoutingRuntimeOrchestratorAdapter router = new RoutingRuntimeOrchestratorAdapter(containerRuntime);
        Host host = Host.create(
                "podman-box",
                "10.0.0.2",
                "linux",
                "",
                true,
                ConnectionType.LOCAL,
                null,
                22,
                null,
                Set.of(RuntimeCapability.PODMAN));

        router.apply(new RuntimeApplyCommand(
                host, workspace, RuntimeCapability.PODMAN, "compose.yml", Optional.empty(), line -> {}));

        verify(containerRuntime).podmanComposeUp(any(), any(), any(), any(), any());
    }

    @Test
    void teardownRoutesByCapability() {
        ContainerRuntimePort containerRuntime = mock(ContainerRuntimePort.class);
        RoutingRuntimeOrchestratorAdapter router = new RoutingRuntimeOrchestratorAdapter(containerRuntime);
        Host host = Host.create(
                "podman-box",
                "10.0.0.2",
                "linux",
                "",
                true,
                ConnectionType.LOCAL,
                null,
                22,
                null,
                Set.of(RuntimeCapability.PODMAN));

        router.teardown(new RuntimeTeardownCommand(
                host, workspace, RuntimeCapability.PODMAN, "compose.yml", Optional.empty(), line -> {}));

        verify(containerRuntime).podmanComposeDown(any(), any(), any(), any(), any());
    }

    @Test
    void rejectsUnsupportedCapability() {
        ContainerRuntimePort containerRuntime = mock(ContainerRuntimePort.class);
        RoutingRuntimeOrchestratorAdapter router = new RoutingRuntimeOrchestratorAdapter(containerRuntime);
        Host host = Host.create("local", "127.0.0.1", "linux", "26", true, ConnectionType.LOCAL, null, 22, null);

        assertThrows(
                DomainException.class,
                () -> router.apply(new RuntimeApplyCommand(
                        host, workspace, RuntimeCapability.K8S, "unused.yml", Optional.empty(), line -> {})));
        verifyNoInteractions(containerRuntime);
    }

    private static void verifyNoInteractionsPodman(ContainerRuntimePort containerRuntime) {
        verify(containerRuntime, org.mockito.Mockito.never())
                .podmanComposeUp(any(), any(), any(), any(), any());
    }
}
