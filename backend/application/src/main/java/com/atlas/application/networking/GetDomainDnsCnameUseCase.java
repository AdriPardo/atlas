package com.atlas.application.networking;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.CloudflareTunnelPort;
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
public class GetDomainDnsCnameUseCase {

    private final DomainRepositoryPort domainRepository;
    private final ProjectAuthorizationService authorizationService;
    private final DnsProviderPort dnsProviderPort;
    private final CloudflareTunnelPort cloudflareTunnelPort;

    @Transactional(readOnly = true)
    public DnsProviderPort.CnameSpec execute(UUID domainId) {
        Domain domain = domainRepository
                .findById(domainId)
                .orElseThrow(() -> new NotFoundException("Domain not found: " + domainId));
        authorizationService.require(domain.getProjectId(), ProjectPermission.READ);
        String target = cloudflareTunnelPort.describe(domain).cnameTarget();
        return dnsProviderPort.describeCname(domain, target);
    }
}
