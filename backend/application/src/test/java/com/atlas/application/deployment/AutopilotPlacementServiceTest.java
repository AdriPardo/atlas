package com.atlas.application.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.domain.host.ConnectionType;
import com.atlas.domain.host.Host;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AutopilotPlacementServiceTest {

    @Mock
    private HostRepositoryPort hostRepository;

    @InjectMocks
    private AutopilotPlacementService service;

    @Test
    void prefersLocalOnlineOverOthers() {
        Host ssh = Host.create("remote", "10.0.0.2", "linux", "", true, ConnectionType.SSH, "root", 22, null);
        Host localOffline = Host.create("old-local", "127.0.0.1", "linux", "", false, ConnectionType.LOCAL, null, 22, null);
        Host localOnline = Host.create("good", "127.0.0.1", "linux", "", true, ConnectionType.LOCAL, null, 22, null);
        when(hostRepository.listForPlacement()).thenReturn(List.of(ssh, localOffline, localOnline));

        Host chosen = service.resolveHost(null);

        assertEquals(localOnline.getId(), chosen.getId());
        verify(hostRepository, never()).save(any());
    }

    @Test
    void seedsDefaultLocalWhenEmpty() {
        when(hostRepository.listForPlacement()).thenReturn(List.of());
        when(hostRepository.findByHostnameIgnoreCase(AutopilotPlacementService.DEFAULT_LOCAL_HOSTNAME))
                .thenReturn(Optional.empty());
        when(hostRepository.save(any(Host.class))).thenAnswer(inv -> inv.getArgument(0));

        Host chosen = service.resolveHost(null);

        assertEquals(AutopilotPlacementService.DEFAULT_LOCAL_HOSTNAME, chosen.getHostname());
        assertEquals(ConnectionType.LOCAL, chosen.getConnectionType());
        verify(hostRepository).save(any(Host.class));
    }

    @Test
    void usesExplicitHostId() {
        UUID hostId = UUID.randomUUID();
        Host host = Host.rehydrate(
                hostId,
                "pinned",
                "10.0.0.9",
                "linux",
                "",
                true,
                ConnectionType.SSH,
                "root",
                22,
                null,
                java.time.Instant.now(),
                java.time.Instant.now());
        when(hostRepository.findById(hostId)).thenReturn(Optional.of(host));

        assertEquals(hostId, service.resolveHost(hostId).getId());
    }
}
