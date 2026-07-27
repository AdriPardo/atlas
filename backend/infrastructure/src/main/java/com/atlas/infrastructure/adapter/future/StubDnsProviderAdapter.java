package com.atlas.infrastructure.adapter.future;

import com.atlas.application.port.out.DnsProviderPort;
import com.atlas.domain.networking.Domain;
import org.springframework.stereotype.Component;

/**
 * Cloudflare / external DNS sync is not wired yet.
 * Returns challenge instructions so operators can create TXT records manually.
 */
@Component
public class StubDnsProviderAdapter implements DnsProviderPort {

    @Override
    public DnsSyncResult syncChallenge(Domain domain) {
        return new DnsSyncResult(
                false,
                "DNS provider stub: create TXT "
                        + domain.dnsTxtName()
                        + " = "
                        + domain.dnsTxtValue()
                        + " (Cloudflare API sync not enabled)");
    }
}
