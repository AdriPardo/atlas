package com.atlas.infrastructure.networking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.port.out.CloudflareTunnelPort;
import com.atlas.domain.networking.Domain;
import com.atlas.infrastructure.config.AtlasProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CloudflareTunnelAdapterTest {

    @Mock
    private CloudflareTunnelHttpGateway httpGateway;

    private AtlasProperties properties;
    private CloudflareTunnelAdapter adapter;

    @BeforeEach
    void setUp() {
        properties = new AtlasProperties();
        properties.getNetworking().setCloudflareZone("atlasops.dev");
        properties.getNetworking().setCloudflareTunnelId("tunnel-abc");
        properties.getNetworking().setCloudflareAccountId("acct-1");
        properties.getNetworking().setCloudflareTunnelOrigin("https://traefik:443");
        properties.getNetworking().setCloudflareTunnelNoTlsVerify(true);
        adapter = new CloudflareTunnelAdapter(properties, httpGateway, new ObjectMapper());
    }

    @Test
    void describeBuildsCopyBlockForZeroTrust() {
        Domain domain = Domain.create(UUID.randomUUID(), "reelpath.atlasops.dev", null);

        CloudflareTunnelPort.TunnelIngressSpec spec = adapter.describe(domain);

        assertEquals("reelpath", spec.subdomain());
        assertEquals("atlasops.dev", spec.zone());
        assertEquals("HTTPS", spec.type());
        assertEquals("traefik:443", spec.originUrl());
        assertTrue(spec.noTlsVerify());
        assertTrue(spec.copyBlock().contains("Subdomain: reelpath"));
        assertTrue(spec.copyBlock().contains("tunnel-abc.cfargotunnel.com"));
    }

    @Test
    void ensureWithoutTokenReturnsManual() {
        Domain domain = Domain.create(UUID.randomUUID(), "app.atlasops.dev", null);

        CloudflareTunnelPort.EnsureResult result = adapter.ensurePublicHostname(domain, Optional.empty());

        assertEquals(CloudflareTunnelPort.EnsureMode.MANUAL, result.mode());
        assertTrue(result.message().contains("cloudflare.api.token"));
    }

    @Test
    void ensureSkipsLocalHostnames() {
        Domain domain = Domain.create(UUID.randomUUID(), "demo.atlas.local", null);

        CloudflareTunnelPort.EnsureResult result =
                adapter.ensurePublicHostname(domain, Optional.of("tok"));

        assertEquals(CloudflareTunnelPort.EnsureMode.SKIPPED, result.mode());
    }

    @Test
    void ensureAppliesNewHostnameViaApi() throws Exception {
        Domain domain = Domain.create(UUID.randomUUID(), "reelpath.atlasops.dev", null);
        String getBody =
                """
                {"success":true,"result":{"config":{"ingress":[
                  {"hostname":"atlas.atlasops.dev","service":"https://traefik:443"},
                  {"service":"http_status:404"}
                ]}}}
                """;
        when(httpGateway.get(anyString(), eq("tok"))).thenReturn(getBody);
        when(httpGateway.put(anyString(), eq("tok"), anyString())).thenReturn("{\"success\":true}");

        CloudflareTunnelPort.EnsureResult result =
                adapter.ensurePublicHostname(domain, Optional.of("tok"));

        assertEquals(CloudflareTunnelPort.EnsureMode.APPLIED, result.mode());
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpGateway).put(anyString(), eq("tok"), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("reelpath.atlasops.dev"));
        assertTrue(bodyCaptor.getValue().contains("http_status:404"));
    }

    @Test
    void ensureDetectsAlreadyPresent() throws Exception {
        Domain domain = Domain.create(UUID.randomUUID(), "atlas.atlasops.dev", null);
        String getBody =
                """
                {"success":true,"result":{"config":{"ingress":[
                  {"hostname":"atlas.atlasops.dev","service":"https://traefik:443"},
                  {"service":"http_status:404"}
                ]}}}
                """;
        when(httpGateway.get(anyString(), eq("tok"))).thenReturn(getBody);

        CloudflareTunnelPort.EnsureResult result =
                adapter.ensurePublicHostname(domain, Optional.of("tok"));

        assertEquals(CloudflareTunnelPort.EnsureMode.ALREADY_PRESENT, result.mode());
    }

    @Test
    void splitHostnameUsesConfiguredZone() {
        var parts = CloudflareTunnelAdapter.splitHostname("a.b.atlasops.dev", "atlasops.dev");
        assertEquals("a.b", parts.subdomain());
        assertEquals("atlasops.dev", parts.zone());
    }
}
