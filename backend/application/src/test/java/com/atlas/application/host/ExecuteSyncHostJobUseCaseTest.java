package com.atlas.application.host;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.port.out.HostConnectorPort;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.domain.host.ConnectionType;
import com.atlas.domain.host.Host;
import com.atlas.domain.runtime.RuntimeCapability;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExecuteSyncHostJobUseCaseTest {

    @Mock
    private HostRepositoryPort hostRepository;

    @Mock
    private HostConnectorPort hostConnector;

    private ExecuteSyncHostJobUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ExecuteSyncHostJobUseCase(hostRepository, hostConnector);
    }

    @Test
    void reachableDockerProbeWritesComposeCapability() {
        Host host = Host.create("atlas-local", "127.0.0.1", "linux", "", true, ConnectionType.LOCAL, null, null, null);
        host.replaceRuntimeCapabilities(Set.of(RuntimeCapability.K8S));
        when(hostRepository.findById(host.getId())).thenReturn(Optional.of(host));
        when(hostConnector.inspect(host))
                .thenReturn(new HostConnectorPort.HostInspection(
                        "atlas-local", "Linux", "27.0.3", true, Set.of("compose")));
        when(hostRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Host saved = useCase.execute(host.getId());

        assertTrue(saved.supportsRuntime(RuntimeCapability.COMPOSE));
        assertFalse(saved.supportsRuntime(RuntimeCapability.K8S));
        assertEquals("27.0.3", saved.getDockerVersion());
        assertTrue(saved.isOnline());
    }

    @Test
    void reachablePodmanProbeWritesPodmanWithoutInventingCompose() {
        Host host = Host.create("podman-box", "10.0.0.2", "linux", "", true, ConnectionType.LOCAL, null, null, null);
        when(hostRepository.findById(host.getId())).thenReturn(Optional.of(host));
        when(hostConnector.inspect(host))
                .thenReturn(new HostConnectorPort.HostInspection(
                        "podman-box", "Linux", "", true, Set.of("podman")));
        when(hostRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Host saved = useCase.execute(host.getId());

        assertEquals(Set.of(RuntimeCapability.PODMAN), saved.runtimeCapabilities());
        assertFalse(saved.supportsRuntime(RuntimeCapability.COMPOSE));
    }

    @Test
    void unreachableKeepsExistingCapabilities() {
        Host host = Host.create(
                "offline",
                "10.0.0.9",
                "linux",
                "26.0.0",
                true,
                ConnectionType.LOCAL,
                null,
                null,
                null,
                Set.of(RuntimeCapability.COMPOSE));
        when(hostRepository.findById(host.getId())).thenReturn(Optional.of(host));
        when(hostConnector.inspect(host))
                .thenReturn(new HostConnectorPort.HostInspection(
                        "offline", "linux", "26.0.0", false, Set.of()));
        when(hostRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Host saved = useCase.execute(host.getId());

        assertTrue(saved.supportsRuntime(RuntimeCapability.COMPOSE));
        assertFalse(saved.isOnline());
        ArgumentCaptor<Host> captor = ArgumentCaptor.forClass(Host.class);
        verify(hostRepository).save(captor.capture());
        assertEquals(Set.of(RuntimeCapability.COMPOSE), captor.getValue().runtimeCapabilities());
    }

    @Test
    void bothRuntimesPersistTogether() {
        Host host = Host.create("hybrid", "10.0.0.3", "linux", "", false, ConnectionType.LOCAL, null, null, null);
        when(hostRepository.findById(any(UUID.class))).thenReturn(Optional.of(host));
        when(hostConnector.inspect(host))
                .thenReturn(new HostConnectorPort.HostInspection(
                        "hybrid", "Linux", "27.1.0", true, Set.of("compose", "podman")));
        when(hostRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Host saved = useCase.execute(host.getId());

        assertTrue(saved.supportsRuntime(RuntimeCapability.COMPOSE));
        assertTrue(saved.supportsRuntime(RuntimeCapability.PODMAN));
    }
}
