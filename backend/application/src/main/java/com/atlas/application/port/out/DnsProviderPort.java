package com.atlas.application.port.out;

import com.atlas.domain.networking.Domain;

/**
 * Optional DNS provider adapter (Cloudflare, etc.). Stub implementations document intent only.
 */
public interface DnsProviderPort {

    DnsSyncResult syncChallenge(Domain domain);

    record DnsSyncResult(boolean applied, String message) {}
}
