package com.atlas.application.networking;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.port.out.DomainRepositoryPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.networking.Domain;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.domain.shared.ConflictException;
import com.atlas.domain.shared.DomainException;
import com.atlas.domain.shared.NotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManageDomainUseCase {

    private final DomainRepositoryPort domainRepository;
    private final ProjectRepositoryPort projectRepository;
    private final ServiceRepositoryPort serviceRepository;
    private final ProjectAuthorizationService authorizationService;
    private final RecordAuditUseCase recordAuditUseCase;

    @Transactional(readOnly = true)
    public List<Domain> list(UUID projectId) {
        authorizationService.require(projectId, ProjectPermission.READ);
        return domainRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public Domain get(UUID domainId) {
        Domain domain = requireDomain(domainId);
        authorizationService.require(domain.getProjectId(), ProjectPermission.READ);
        return domain;
    }

    @Transactional
    public Domain create(UUID projectId, String hostname, UUID serviceId) {
        authorizationService.require(projectId, ProjectPermission.WRITE);
        if (projectRepository.findById(projectId).isEmpty()) {
            throw new NotFoundException("Project not found: " + projectId);
        }
        validateService(projectId, serviceId);
        if (domainRepository.existsByProjectIdAndHostnameIgnoreCase(projectId, hostname.trim())) {
            throw new ConflictException("Domain hostname already registered on project");
        }
        Domain saved = domainRepository.save(Domain.create(projectId, hostname, serviceId));
        recordAuditUseCase.execute(
                "DOMAIN_CREATE",
                "domain",
                saved.getId(),
                "{\"projectId\":\"" + projectId + "\",\"hostname\":\"" + saved.getHostname() + "\"}");
        return saved;
    }

    @Transactional
    public Domain update(UUID domainId, String hostname, UUID serviceId) {
        Domain domain = requireDomain(domainId);
        authorizationService.require(domain.getProjectId(), ProjectPermission.WRITE);
        validateService(domain.getProjectId(), serviceId);
        if (domainRepository.existsByProjectIdAndHostnameIgnoreCaseAndIdNot(
                domain.getProjectId(), hostname.trim(), domainId)) {
            throw new ConflictException("Domain hostname already registered on project");
        }
        domain.update(hostname, serviceId);
        Domain saved = domainRepository.save(domain);
        recordAuditUseCase.execute(
                "DOMAIN_UPDATE",
                "domain",
                saved.getId(),
                "{\"hostname\":\"" + saved.getHostname() + "\"}");
        return saved;
    }

    @Transactional
    public void delete(UUID domainId) {
        Domain domain = requireDomain(domainId);
        authorizationService.require(domain.getProjectId(), ProjectPermission.WRITE);
        domainRepository.deleteById(domainId);
        recordAuditUseCase.execute(
                "DOMAIN_DELETE",
                "domain",
                domainId,
                "{\"projectId\":\"" + domain.getProjectId() + "\",\"hostname\":\"" + domain.getHostname() + "\"}");
    }

    private Domain requireDomain(UUID domainId) {
        return domainRepository
                .findById(domainId)
                .orElseThrow(() -> new NotFoundException("Domain not found: " + domainId));
    }

    private void validateService(UUID projectId, UUID serviceId) {
        if (serviceId == null) {
            return;
        }
        ServiceUnit service = serviceRepository
                .findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found: " + serviceId));
        if (!service.getProjectId().equals(projectId)) {
            throw new DomainException("Service does not belong to project");
        }
    }
}
