package com.atlas.application.networking;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.port.out.CloudflareTunnelPort;
import com.atlas.application.port.out.DomainRepositoryPort;
import com.atlas.application.secret.ResolveSecretValueUseCase;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.networking.Domain;
import com.atlas.domain.shared.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnsureDomainTunnelIngressUseCase {

    private final DomainRepositoryPort domainRepository;
    private final ProjectAuthorizationService authorizationService;
    private final CloudflareTunnelPort cloudflareTunnelPort;
    private final ResolveSecretValueUseCase resolveSecretValue;
    private final RecordAuditUseCase recordAuditUseCase;

    @Transactional
    public CloudflareTunnelPort.EnsureResult execute(UUID domainId) {
        Domain domain = domainRepository
                .findById(domainId)
                .orElseThrow(() -> new NotFoundException("Domain not found: " + domainId));
        authorizationService.require(domain.getProjectId(), ProjectPermission.WRITE);
        return ensure(domain);
    }

    /**
     * Worker / Autopilot path — no interactive user; still resolves the org secret token when
     * present.
     */
    @Transactional
    public CloudflareTunnelPort.EnsureResult executeAsSystem(Domain domain) {
        return ensure(domain);
    }

    private CloudflareTunnelPort.EnsureResult ensure(Domain domain) {
        Optional<String> token =
                resolveSecretValue.forProject(domain.getProjectId(), CloudflareTunnelPort.API_TOKEN_SECRET_NAME);
        CloudflareTunnelPort.EnsureResult result = cloudflareTunnelPort.ensurePublicHostname(domain, token);
        recordAuditUseCase.execute(
                "DOMAIN_TUNNEL_ENSURE",
                "domain",
                domain.getId(),
                "{\"mode\":\"" + result.mode() + "\",\"hostname\":\"" + domain.getHostname() + "\"}");
        return result;
    }
}
