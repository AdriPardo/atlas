package com.atlas.infrastructure.networking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.port.out.DnsProviderPort;
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
class CloudflareDnsAdapterTest {

    @Mock
    private CloudflareTunnelHttpGateway httpGateway;

    private AtlasProperties properties;
    private CloudflareDnsAdapter adapter;

    @BeforeEach
    void setUp() {
        properties = new AtlasProperties();
        properties.getNetworking().setCloudflareZone("atlasops.dev");
        properties.getNetworking().setCloudflareZoneId("zone-1");
        properties.getNetworking().setCloudflareTunnelId("tunnel-abc");
        adapter = new CloudflareDnsAdapter(properties, httpGateway, new ObjectMapper());
    }

    @Test
    void describeBuildsCopyBlock() {
        Domain domain = Domain.create(UUID.randomUUID(), "reelpath.atlasops.dev", null);

        DnsProviderPort.CnameSpec spec =
                adapter.describeCname(domain, "tunnel-abc.cfargotunnel.com");

        assertEquals("reelpath.atlasops.dev", spec.hostname());
        assertEquals("atlasops.dev", spec.zone());
        assertTrue(spec.proxied());
        assertTrue(spec.copyBlock().contains("tunnel-abc.cfargotunnel.com"));
    }

    @Test
    void ensureWithoutTokenReturnsManual() {
        Domain domain = Domain.create(UUID.randomUUID(), "app.atlasops.dev", null);

        DnsProviderPort.CnameEnsureResult result =
                adapter.ensureCname(domain, "tunnel-abc.cfargotunnel.com", Optional.empty());

        assertEquals(DnsProviderPort.CnameEnsureMode.MANUAL, result.mode());
        assertTrue(result.message().contains("cloudflare.api.token"));
    }

    @Test
    void ensureSkipsLocalHostnames() {
        Domain domain = Domain.create(UUID.randomUUID(), "demo.atlas.local", null);

        DnsProviderPort.CnameEnsureResult result =
                adapter.ensureCname(domain, "tunnel-abc.cfargotunnel.com", Optional.of("tok"));

        assertEquals(DnsProviderPort.CnameEnsureMode.SKIPPED, result.mode());
    }

    @Test
    void ensureCreatesCnameWhenMissing() throws Exception {
        Domain domain = Domain.create(UUID.randomUUID(), "reelpath.atlasops.dev", null);
        when(httpGateway.get(contains("/dns_records"), eq("tok")))
                .thenReturn("{\"success\":true,\"result\":[]}");
        when(httpGateway.post(anyString(), eq("tok"), anyString()))
                .thenReturn("{\"success\":true}");

        DnsProviderPort.CnameEnsureResult result =
                adapter.ensureCname(domain, "tunnel-abc.cfargotunnel.com", Optional.of("tok"));

        assertEquals(DnsProviderPort.CnameEnsureMode.APPLIED, result.mode());
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpGateway).post(contains("/dns_records"), eq("tok"), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains("\"type\":\"CNAME\""));
        assertTrue(bodyCaptor.getValue().contains("tunnel-abc.cfargotunnel.com"));
        assertTrue(bodyCaptor.getValue().contains("\"proxied\":true"));
        verify(httpGateway, never()).patch(anyString(), anyString(), anyString());
    }

    @Test
    void ensureDetectsAlreadyPresent() throws Exception {
        Domain domain = Domain.create(UUID.randomUUID(), "reelpath.atlasops.dev", null);
        when(httpGateway.get(contains("/dns_records"), eq("tok")))
                .thenReturn(
                        """
                        {"success":true,"result":[{
                          "id":"rec-1",
                          "type":"CNAME",
                          "name":"reelpath.atlasops.dev",
                          "content":"tunnel-abc.cfargotunnel.com",
                          "proxied":true
                        }]}
                        """);

        DnsProviderPort.CnameEnsureResult result =
                adapter.ensureCname(domain, "tunnel-abc.cfargotunnel.com", Optional.of("tok"));

        assertEquals(DnsProviderPort.CnameEnsureMode.ALREADY_PRESENT, result.mode());
        verify(httpGateway, never()).post(anyString(), anyString(), anyString());
        verify(httpGateway, never()).patch(anyString(), anyString(), anyString());
    }

    @Test
    void ensureUpdatesWrongTarget() throws Exception {
        Domain domain = Domain.create(UUID.randomUUID(), "reelpath.atlasops.dev", null);
        when(httpGateway.get(contains("/dns_records"), eq("tok")))
                .thenReturn(
                        """
                        {"success":true,"result":[{
                          "id":"rec-1",
                          "type":"CNAME",
                          "name":"reelpath.atlasops.dev",
                          "content":"old.cfargotunnel.com",
                          "proxied":true
                        }]}
                        """);
        when(httpGateway.patch(anyString(), eq("tok"), anyString()))
                .thenReturn("{\"success\":true}");

        DnsProviderPort.CnameEnsureResult result =
                adapter.ensureCname(domain, "tunnel-abc.cfargotunnel.com", Optional.of("tok"));

        assertEquals(DnsProviderPort.CnameEnsureMode.UPDATED, result.mode());
        verify(httpGateway).patch(contains("/dns_records/rec-1"), eq("tok"), anyString());
    }
}
