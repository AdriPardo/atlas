package com.atlas.application.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.application.port.out.VmProvisionerPort;
import com.atlas.application.secret.ResolveSecretValueUseCase;
import com.atlas.domain.deployment.PlacementMode;
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

    @Mock
    private VmProvisionerPort vmProvisioner;

    @Mock
    private ResolveSecretValueUseCase resolveSecretValue;

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
        verify(vmProvisioner, never()).provision(any(), any());
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
        verify(vmProvisioner, never()).provision(any(), any());
    }

    @Test
    void isolatedFallsBackToSharedWhenProvisionerStubbed() {
        UUID projectId = UUID.randomUUID();
        Host local = Host.create("good", "127.0.0.1", "linux", "", true, ConnectionType.LOCAL, null, 22, null);
        when(resolveSecretValue.forProject(projectId, VmProvisionerPort.API_TOKEN_SECRET_NAME))
                .thenReturn(Optional.empty());
        when(vmProvisioner.provision(any(), eq(Optional.empty())))
                .thenReturn(VmProvisionerPort.ProvisionResult.of(
                        VmProvisionerPort.ProvisionMode.STUBBED, "no token"));
        when(hostRepository.listForPlacement()).thenReturn(List.of(local));

        AutopilotPlacementService.PlacementResult result =
                service.resolveHost(null, PlacementMode.ISOLATED, projectId, "demo");

        assertEquals(local.getId(), result.host().getId());
        assertEquals(PlacementMode.SHARED, result.effectiveMode());
        assertEquals(VmProvisionerPort.ProvisionMode.STUBBED, result.provisionMode());
        verify(vmProvisioner).provision(any(), eq(Optional.empty()));
    }

    @Test
    void isolatedRegistersHostWhenProvisionerCreatesVm() {
        UUID projectId = UUID.randomUUID();
        when(resolveSecretValue.forProject(projectId, VmProvisionerPort.API_TOKEN_SECRET_NAME))
                .thenReturn(Optional.of("token"));
        VmProvisionerPort.VmDescriptor vm = new VmProvisionerPort.VmDescriptor(
                "301", "atlas-demo", "10.0.0.50", "pve", "atlas", 22);
        when(vmProvisioner.provision(any(), eq(Optional.of("token"))))
                .thenReturn(VmProvisionerPort.ProvisionResult.of(
                        VmProvisionerPort.ProvisionMode.CREATED, "cloned", vm));
        when(hostRepository.findByHostnameIgnoreCase("atlas-demo")).thenReturn(Optional.empty());
        when(hostRepository.save(any(Host.class))).thenAnswer(inv -> inv.getArgument(0));

        AutopilotPlacementService.PlacementResult result =
                service.resolveHost(null, PlacementMode.ISOLATED, projectId, "demo");

        assertEquals(PlacementMode.ISOLATED, result.effectiveMode());
        assertEquals("atlas-demo", result.host().getHostname());
        assertEquals("10.0.0.50", result.host().getIp());
        assertEquals(ConnectionType.SSH, result.host().getConnectionType());
        assertEquals(VmProvisionerPort.ProvisionMode.CREATED, result.provisionMode());
        assertNotNull(result.reason());
        verify(hostRepository).save(any(Host.class));
        verify(hostRepository, never()).listForPlacement();
    }
}
