package com.atlas.application.port.out;

import com.atlas.domain.networking.Domain;
import java.util.Optional;

/**
 * Cloudflare Tunnel public-hostname assist for Autopilot PUBLIC exposure.
 *
 * <p>Always produces an exact Zero Trust ingress spec for copy/paste. When account/tunnel ids and
 * an API token are available, {@link #ensurePublicHostname} may apply the hostname remotely.
 */
public interface CloudflareTunnelPort {

    /** Logical org/global secret name for a Cloudflare API token with Tunnel Edit. */
    String API_TOKEN_SECRET_NAME = "cloudflare.api.token";

    TunnelIngressSpec describe(Domain domain);

    EnsureResult ensurePublicHostname(Domain domain, Optional<String> apiToken);

    enum EnsureMode {
        /** Ingress rule created/updated via Cloudflare API. */
        APPLIED,
        /** Hostname already present in tunnel config. */
        ALREADY_PRESENT,
        /** No API credentials / incomplete config — operator must paste Zero Trust fields. */
        MANUAL,
        /** Hostname is not a candidate for public tunnel (e.g. *.atlas.local). */
        SKIPPED,
        /** API attempted but failed (message explains). */
        FAILED
    }

    record TunnelIngressSpec(
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
            String zeroTrustHint) {}

    record EnsureResult(EnsureMode mode, String message, TunnelIngressSpec ingress) {}
}
