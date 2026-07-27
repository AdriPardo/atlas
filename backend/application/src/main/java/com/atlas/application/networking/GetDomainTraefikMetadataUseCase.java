package com.atlas.application.networking;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.DomainRepositoryPort;
import com.atlas.application.port.out.TraefikMetadataPort;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.networking.Domain;
import com.atlas.domain.shared.NotFoundException;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetDomainTraefikMetadataUseCase {

    private final DomainRepositoryPort domainRepository;
    private final ProjectAuthorizationService authorizationService;
    private final TraefikMetadataPort traefikMetadataPort;

    @Transactional(readOnly = true)
    public TraefikMetadataPort.TraefikRouteMetadata execute(UUID domainId) {
        Domain domain = domainRepository
                .findById(domainId)
                .orElseThrow(() -> new NotFoundException("Domain not found: " + domainId));
        authorizationService.require(domain.getProjectId(), ProjectPermission.READ);
        String routerName = "atlas-" + domain.getHostname().replace('.', '-').toLowerCase(Locale.ROOT);
        return traefikMetadataPort.metadataFor(domain, routerName);
    }
}
