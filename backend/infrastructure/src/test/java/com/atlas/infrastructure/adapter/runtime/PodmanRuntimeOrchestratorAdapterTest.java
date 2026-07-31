package com.atlas.infrastructure.adapter.runtime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PodmanRuntimeOrchestratorAdapterTest {

    @TempDir
    Path workspace;

    @Test
    void applyDelegatesToPodmanComposeUp() {
        ContainerRuntimePort containerRuntime = mock(ContainerRuntimePort.class);
        PodmanRuntimeOrchestratorAdapter adapter = new PodmanRuntimeOrchestratorAdapter(containerRuntime);
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
                java.util.Set.of(RuntimeCapability.PODMAN));
        List<String> logs = new ArrayList<>();

        adapter.apply(new RuntimeApplyCommand(
                host, workspace, RuntimeCapability.PODMAN, "compose.yml", Optional.empty(), logs::add));

        verify(containerRuntime)
                .podmanComposeUp(eq(host), eq(workspace), eq("compose.yml"), eq(Optional.empty()), any());
        assertTrue(logs.stream().anyMatch(line -> line.contains("apply via podman")));
    }

    @Test
    void teardownDelegatesToPodmanComposeDown() {
        ContainerRuntimePort containerRuntime = mock(ContainerRuntimePort.class);
        PodmanRuntimeOrchestratorAdapter adapter = new PodmanRuntimeOrchestratorAdapter(containerRuntime);
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
                java.util.Set.of(RuntimeCapability.PODMAN));

        adapter.teardown(new RuntimeTeardownCommand(
                host, workspace, RuntimeCapability.PODMAN, "compose.yml", Optional.empty(), line -> {}));

        verify(containerRuntime)
                .podmanComposeDown(eq(host), eq(workspace), eq("compose.yml"), eq(Optional.empty()), any());
    }

    @Test
    void rejectsNonPodmanCapability() {
        ContainerRuntimePort containerRuntime = mock(ContainerRuntimePort.class);
        PodmanRuntimeOrchestratorAdapter adapter = new PodmanRuntimeOrchestratorAdapter(containerRuntime);
        Host host = Host.create("local", "127.0.0.1", "linux", "26", true, ConnectionType.LOCAL, null, 22, null);

        assertThrows(
                DomainException.class,
                () -> adapter.apply(new RuntimeApplyCommand(
                        host, workspace, RuntimeCapability.COMPOSE, "unused.yml", Optional.empty(), line -> {})));
        verifyNoInteractions(containerRuntime);
    }
}
