package com.atlas.infrastructure.adapter.future;

import com.atlas.application.port.out.DnsProviderPort;
import com.atlas.domain.networking.Domain;
import java.util.Optional;

/**
 * Fallback DNS provider kept for reference / tests. Production uses {@code CloudflareDnsAdapter}.
 */
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

    @Override
    public CnameSpec describeCname(Domain domain, String cnameTarget) {
        String hostname = domain.getHostname();
        String target = cnameTarget == null || cnameTarget.isBlank()
                ? "<tunnel-id>.cfargotunnel.com"
                : cnameTarget.trim();
        String copy = "Type: CNAME\nName: " + hostname + "\nTarget: " + target + "\nProxy: proxied";
        return new CnameSpec(hostname, "", hostname, target, true, copy);
    }

    @Override
    public CnameEnsureResult ensureCname(Domain domain, String cnameTarget, Optional<String> apiToken) {
        CnameSpec spec = describeCname(domain, cnameTarget);
        return new CnameEnsureResult(
                CnameEnsureMode.MANUAL,
                "DNS provider stub: create CNAME " + spec.hostname() + " → " + spec.cnameTarget(),
                spec);
    }
}
