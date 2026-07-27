package com.atlas.application.port.out;

import com.atlas.domain.networking.Domain;
import java.util.Optional;

/**
 * DNS provider adapter (Cloudflare zone DNS). Challenge TXT remains instructional; Autopilot PUBLIC
 * uses {@link #ensureCname} to point the hostname at the Tunnel CNAME target.
 */
public interface DnsProviderPort {

    /** Same logical secret as Tunnel assist — token should include Zone DNS Edit (+ Tunnel Edit if shared). */
    String API_TOKEN_SECRET_NAME = "cloudflare.api.token";

    DnsSyncResult syncChallenge(Domain domain);

    CnameSpec describeCname(Domain domain, String cnameTarget);

    CnameEnsureResult ensureCname(Domain domain, String cnameTarget, Optional<String> apiToken);

    enum CnameEnsureMode {
        /** CNAME created via DNS API. */
        APPLIED,
        /** Existing CNAME content/proxied updated. */
        UPDATED,
        /** Record already matches target (proxied). */
        ALREADY_PRESENT,
        /** Missing zone/tunnel/token — operator must create the record manually. */
        MANUAL,
        /** Hostname is not a public DNS candidate (e.g. *.atlas.local). */
        SKIPPED,
        /** API attempted but failed (message explains). */
        FAILED
    }

    record DnsSyncResult(boolean applied, String message) {}

    record CnameSpec(
            String hostname,
            String zone,
            String recordName,
            String cnameTarget,
            boolean proxied,
            String copyBlock) {}

    record CnameEnsureResult(CnameEnsureMode mode, String message, CnameSpec spec) {}
}
