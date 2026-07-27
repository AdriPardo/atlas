package com.atlas.application.networking;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.port.out.DnsProviderPort;
import com.atlas.application.port.out.DomainRepositoryPort;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.networking.Domain;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VerifyDomainUseCase {

    private final DomainRepositoryPort domainRepository;
    private final ProjectAuthorizationService authorizationService;
    private final DnsProviderPort dnsProviderPort;
    private final RecordAuditUseCase recordAuditUseCase;

    @Transactional
    public Domain execute(UUID domainId) {
        Domain domain = domainRepository
                .findById(domainId)
                .orElseThrow(() -> new NotFoundException("Domain not found: " + domainId));
        authorizationService.require(domain.getProjectId(), ProjectPermission.WRITE);

        DnsProviderPort.DnsSyncResult sync = dnsProviderPort.syncChallenge(domain);
        domain.markVerified();
        Domain saved = domainRepository.save(domain);
        recordAuditUseCase.execute(
                "DOMAIN_VERIFY",
                "domain",
                saved.getId(),
                "{\"hostname\":\""
                        + saved.getHostname()
                        + "\",\"dnsApplied\":"
                        + sync.applied()
                        + ",\"message\":\""
                        + escape(sync.message())
                        + "\"}");
        return saved;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
