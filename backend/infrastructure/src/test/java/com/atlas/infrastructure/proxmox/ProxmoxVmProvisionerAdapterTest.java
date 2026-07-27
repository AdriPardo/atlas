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
import static org.mockito.Mockito.times;
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
        properties.getProxmox().setGuestReadyTimeoutSeconds(5);
        properties.getProxmox().setGuestReadyPollIntervalMs(1);
        adapter = new ProxmoxVmProvisionerAdapter(
                properties, httpGateway, new ObjectMapper(), ms -> {});
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
    void clonesStartsAndResolvesGuestAgentIp() throws Exception {
        properties.getProxmox().setCloneEnabled(true);
        properties.getProxmox().setStorage("local-lvm");
        when(httpGateway.get(contains("/api2/json/version"), eq("tok"), eq(true)))
                .thenReturn("{\"data\":{\"version\":\"8.2.0\"}}");
        when(httpGateway.postForm(contains("/clone"), eq("tok"), anyMap(), eq(true)))
                .thenReturn("{\"data\":\"UPID:pve:0000:qmclone\"}");
        when(httpGateway.get(contains("/tasks/"), eq("tok"), eq(true)))
                .thenReturn("{\"data\":{\"status\":\"stopped\",\"exitstatus\":\"OK\"}}");
        when(httpGateway.postForm(contains("/status/start"), eq("tok"), anyMap(), eq(true)))
                .thenReturn("{\"data\":\"UPID:pve:0001:qmstart\"}");
        when(httpGateway.get(contains("/agent/network-get-interfaces"), eq("tok"), eq(true)))
                .thenReturn(
                        """
                        {"data":{"result":[
                          {"name":"lo","ip-addresses":[{"ip-address":"127.0.0.1","ip-address-type":"ipv4"}]},
                          {"name":"eth0","ip-addresses":[{"ip-address":"10.0.0.77","ip-address-type":"ipv4"}]}
                        ]}}
                        """);

        VmProvisionerPort.ProvisionResult result = adapter.provision(
                new VmProvisionerPort.ProvisionRequest(UUID.randomUUID(), "demo-app", "demo-app"),
                Optional.of("tok"));

        assertEquals(VmProvisionerPort.ProvisionMode.CREATED, result.mode());
        assertTrue(result.vm().isPresent());
        assertEquals("10.0.0.77", result.vm().get().ip());
        assertTrue(result.vm().get().hostname().startsWith("atlas-"));
        assertTrue(result.message().contains("ip=10.0.0.77"));
        verify(httpGateway).postForm(contains("/qemu/9000/clone"), eq("tok"), anyMap(), eq(true));
        verify(httpGateway).postForm(contains("/status/start"), eq("tok"), anyMap(), eq(true));
        verify(httpGateway, times(2)).get(contains("/tasks/"), eq("tok"), eq(true));
    }

    @Test
    void fallsBackToDefaultGuestIpWhenAgentTimesOut() throws Exception {
        properties.getProxmox().setCloneEnabled(true);
        properties.getProxmox().setDefaultGuestIp("10.0.0.99");
        properties.getProxmox().setGuestReadyTimeoutSeconds(0);
        when(httpGateway.get(contains("/api2/json/version"), eq("tok"), eq(true)))
                .thenReturn("{\"data\":{\"version\":\"8.2.0\"}}");
        when(httpGateway.postForm(contains("/clone"), eq("tok"), anyMap(), eq(true)))
                .thenReturn("{\"data\":null}");
        when(httpGateway.postForm(contains("/status/start"), eq("tok"), anyMap(), eq(true)))
                .thenReturn("{\"data\":null}");
        when(httpGateway.get(contains("/agent/network-get-interfaces"), eq("tok"), eq(true)))
                .thenThrow(new java.io.IOException("QEMU guest agent is not running"));

        VmProvisionerPort.ProvisionResult result = adapter.provision(
                new VmProvisionerPort.ProvisionRequest(UUID.randomUUID(), "demo", "demo"),
                Optional.of("tok"));

        // timeout 0 → loop may not run; defaultGuestIp used after wait returns
        assertEquals(VmProvisionerPort.ProvisionMode.CREATED, result.mode());
        assertEquals("10.0.0.99", result.vm().orElseThrow().ip());
    }

    @Test
    void stubbedWhenGuestIpUnknown() throws Exception {
        properties.getProxmox().setCloneEnabled(true);
        properties.getProxmox().setGuestReadyTimeoutSeconds(0);
        when(httpGateway.get(contains("/api2/json/version"), eq("tok"), eq(true)))
                .thenReturn("{\"data\":{\"version\":\"8.2.0\"}}");
        when(httpGateway.postForm(anyString(), eq("tok"), anyMap(), eq(true))).thenReturn("{}");
        when(httpGateway.get(contains("/agent/network-get-interfaces"), eq("tok"), eq(true)))
                .thenThrow(new java.io.IOException("agent down"));

        VmProvisionerPort.ProvisionResult result = adapter.provision(
                new VmProvisionerPort.ProvisionRequest(UUID.randomUUID(), "demo", "demo"),
                Optional.of("tok"));

        assertEquals(VmProvisionerPort.ProvisionMode.STUBBED, result.mode());
        assertTrue(result.message().contains("guest IP unknown"));
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

    @Test
    void extractGuestIpv4SkipsLoopback() throws Exception {
        var root = new ObjectMapper()
                .readTree(
                        """
                        {"data":{"result":[
                          {"name":"lo","ip-addresses":[{"ip-address":"127.0.0.1","ip-address-type":"ipv4"}]},
                          {"name":"eth0","ip-addresses":[
                            {"ip-address":"fe80::1","ip-address-type":"ipv6"},
                            {"ip-address":"192.168.1.50","ip-address-type":"ipv4"}
                          ]}
                        ]}}
                        """);
        assertEquals(Optional.of("192.168.1.50"), ProxmoxVmProvisionerAdapter.extractGuestIpv4(root));
    }
}
