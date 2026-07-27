package com.atlas.api.dto.response;

public record TunnelIngressResponse(
        String hostname,
        String subdomain,
        String zone,
        String type,
        String originUrl,
        String originService,
        boolean noTlsVerify,
        String tunnelId,
        String cnameTarget,
        String copyBlock,
        String zeroTrustHint,
        String mode,
        String message) {}
