package com.atlas.application.networking;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.port.out.CloudflareTunnelPort;
import com.atlas.application.port.out.DnsProviderPort;
import com.atlas.application.port.out.DomainRepositoryPort;
import com.atlas.application.secret.ResolveSecretValueUseCase;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.networking.Domain;
import com.atlas.domain.networking.DomainStatus;
import com.atlas.domain.shared.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnsureDomainDnsCnameUseCase {

    private final DomainRepositoryPort domainRepository;
    private final ProjectAuthorizationService authorizationService;
    private final DnsProviderPort dnsProviderPort;
    private final CloudflareTunnelPort cloudflareTunnelPort;
    private final ResolveSecretValueUseCase resolveSecretValue;
    private final RecordAuditUseCase recordAuditUseCase;

    @Transactional
    public DnsProviderPort.CnameEnsureResult execute(UUID domainId) {
        Domain domain = domainRepository
                .findById(domainId)
                .orElseThrow(() -> new NotFoundException("Domain not found: " + domainId));
        authorizationService.require(domain.getProjectId(), ProjectPermission.WRITE);
        return ensure(domain);
    }

    /** Worker / Autopilot path — no interactive user. */
    @Transactional
    public DnsProviderPort.CnameEnsureResult executeAsSystem(Domain domain) {
        return ensure(domain);
    }

    private DnsProviderPort.CnameEnsureResult ensure(Domain domain) {
        String cnameTarget = cloudflareTunnelPort.describe(domain).cnameTarget();
        Optional<String> token =
                resolveSecretValue.forProject(domain.getProjectId(), DnsProviderPort.API_TOKEN_SECRET_NAME);
        DnsProviderPort.CnameEnsureResult result = dnsProviderPort.ensureCname(domain, cnameTarget, token);

        if (isSuccess(result.mode()) && domain.getStatus() == DomainStatus.PENDING_DNS) {
            domain.markVerified();
            domainRepository.save(domain);
        }

        recordAuditUseCase.execute(
                "DOMAIN_DNS_CNAME_ENSURE",
                "domain",
                domain.getId(),
                "{\"mode\":\"" + result.mode() + "\",\"hostname\":\"" + domain.getHostname() + "\"}");
        return result;
    }

    private static boolean isSuccess(DnsProviderPort.CnameEnsureMode mode) {
        return mode == DnsProviderPort.CnameEnsureMode.APPLIED
                || mode == DnsProviderPort.CnameEnsureMode.UPDATED
                || mode == DnsProviderPort.CnameEnsureMode.ALREADY_PRESENT;
    }
}
