package com.atlas.infrastructure.networking;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class CloudflareApiErrorMessagesTest {

    @Test
    void detectsForbiddenGet() {
        assertTrue(CloudflareApiErrorMessages.isForbidden(
                new IOException("Cloudflare GET 403: {\"success\":false,\"errors\":[{\"code\":10000}]}")));
    }

    @Test
    void detectsForbiddenPut() {
        assertTrue(CloudflareApiErrorMessages.isForbidden(
                new IOException("Cloudflare PUT 403: Authentication error")));
    }

    @Test
    void ignoresOtherStatuses() {
        assertFalse(CloudflareApiErrorMessages.isForbidden(
                new IOException("Cloudflare GET 500: internal error")));
        assertFalse(CloudflareApiErrorMessages.isForbidden(new IOException("timeout")));
        assertFalse(CloudflareApiErrorMessages.isForbidden(null));
    }

    @Test
    void failedEnsureMessageMapsScopes() {
        String msg = CloudflareApiErrorMessages.failedEnsureMessage(
                "Cloudflare Tunnel API", new IOException("Cloudflare GET 403: denied"));
        assertTrue(msg.contains("token scopes insufficient"));
        assertTrue(msg.contains("cloudflare.api.token"));
        assertTrue(msg.contains("Org secrets"));
    }

    @Test
    void failedEnsureMessageKeepsOpaqueErrors() {
        String msg = CloudflareApiErrorMessages.failedEnsureMessage(
                "Cloudflare DNS API", new IOException("Cloudflare POST 429: rate limited"));
        assertTrue(msg.contains("429"));
        assertFalse(msg.contains("token scopes insufficient"));
    }
}
