package com.atlas.infrastructure.proxmox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.port.out.VmProvisionerPort;
import com.atlas.infrastructure.config.AtlasProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProxmoxVmProvisionerAdapterTest {

    @Mock
    private ProxmoxHttpGateway httpGateway;

    private AtlasProperties properties;
    private ProxmoxVmProvisionerAdapter adapter;

    @BeforeEach
    void setUp() {
        properties = new AtlasProperties();
        properties.getProxmox().setApiUrl("https://pve.example:8006");
        properties.getProxmox().setNode("pve");
        properties.getProxmox().setTemplateVmid("9000");
        properties.getProxmox().setCloneEnabled(false);
        properties.getProxmox().setInsecureTls(true);
        adapter = new ProxmoxVmProvisionerAdapter(properties, httpGateway, new ObjectMapper());
    }

    @Test
    void notConfiguredWithoutUrl() {
        properties.getProxmox().setApiUrl("");
        assertFalse(adapter.isConfigured());
        VmProvisionerPort.ProvisionResult result = adapter.provision(
                new VmProvisionerPort.ProvisionRequest(UUID.randomUUID(), "demo", "demo"),
                Optional.of("tok"));
        assertEquals(VmProvisionerPort.ProvisionMode.STUBBED, result.mode());
    }

    @Test
    void stubbedWithoutToken() {
        assertTrue(adapter.isConfigured());
        VmProvisionerPort.ProvisionResult result = adapter.provision(
                new VmProvisionerPort.ProvisionRequest(UUID.randomUUID(), "demo", "demo"), Optional.empty());
        assertEquals(VmProvisionerPort.ProvisionMode.STUBBED, result.mode());
        assertTrue(result.message().contains("proxmox.api.token"));
    }

    @Test
    void probesVersionWhenCloneDisabled() throws Exception {
        when(httpGateway.get(contains("/api2/json/version"), eq("tok"), eq(true)))
                .thenReturn("{\"data\":{\"version\":\"8.2.0\"}}");

        VmProvisionerPort.ProvisionResult result = adapter.provision(
                new VmProvisionerPort.ProvisionRequest(UUID.randomUUID(), "demo", "demo"),
                Optional.of("tok"));

        assertEquals(VmProvisionerPort.ProvisionMode.STUBBED, result.mode());
        assertTrue(result.message().contains("8.2.0"));
        assertTrue(result.message().contains("clone disabled"));
        verify(httpGateway, never()).postForm(anyString(), anyString(), anyMap(), anyBoolean());
    }

    @Test
    void clonesWhenEnabledAndGuestIpSet() throws Exception {
        properties.getProxmox().setCloneEnabled(true);
        properties.getProxmox().setDefaultGuestIp("10.0.0.77");
        properties.getProxmox().setStorage("local-lvm");
        when(httpGateway.get(contains("/api2/json/version"), eq("tok"), eq(true)))
                .thenReturn("{\"data\":{\"version\":\"8.2.0\"}}");
        when(httpGateway.postForm(contains("/clone"), eq("tok"), anyMap(), eq(true)))
                .thenReturn("{\"data\":\"UPID:pve:...\"}");

        VmProvisionerPort.ProvisionResult result = adapter.provision(
                new VmProvisionerPort.ProvisionRequest(UUID.randomUUID(), "demo-app", "demo-app"),
                Optional.of("tok"));

        assertEquals(VmProvisionerPort.ProvisionMode.CREATED, result.mode());
        assertTrue(result.vm().isPresent());
        assertEquals("10.0.0.77", result.vm().get().ip());
        assertTrue(result.vm().get().hostname().startsWith("atlas-"));
        verify(httpGateway).postForm(contains("/qemu/9000/clone"), eq("tok"), anyMap(), eq(true));
    }

    @Test
    void unavailableWhenApiFails() throws Exception {
        when(httpGateway.get(anyString(), anyString(), anyBoolean()))
                .thenThrow(new java.io.IOException("connection refused"));

        VmProvisionerPort.ProvisionResult result = adapter.provision(
                new VmProvisionerPort.ProvisionRequest(UUID.randomUUID(), "demo", "demo"),
                Optional.of("tok"));

        assertEquals(VmProvisionerPort.ProvisionMode.UNAVAILABLE, result.mode());
    }

    @Test
    void sanitizeHostnamePrefixesAtlas() {
        assertEquals("atlas-demo", ProxmoxVmProvisionerAdapter.sanitizeHostname("demo", null));
        assertEquals("atlas-my-svc", ProxmoxVmProvisionerAdapter.sanitizeHostname(null, "My Svc"));
    }
}
