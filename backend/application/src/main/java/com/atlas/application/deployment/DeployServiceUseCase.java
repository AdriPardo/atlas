package com.atlas.application.deployment;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.job.EnqueueJobUseCase;
import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.DomainRepositoryPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.deployment.PlacementMode;
import com.atlas.domain.host.Host;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobType;
import com.atlas.domain.networking.Domain;
import com.atlas.domain.project.Project;
import com.atlas.domain.project.ProjectStatus;
import com.atlas.domain.service.ServiceExposure;
import com.atlas.domain.service.ServiceStatus;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.domain.shared.NotFoundException;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeployServiceUseCase {

    private final ServiceRepositoryPort serviceRepository;
    private final ProjectRepositoryPort projectRepository;
    private final DeploymentRepositoryPort deploymentRepository;
    private final DomainRepositoryPort domainRepository;
    private final AutopilotPlacementService autopilotPlacementService;
    private final EnqueueJobUseCase enqueueJobUseCase;
    private final ProjectAuthorizationService authorizationService;
    private final RecordAuditUseCase recordAuditUseCase;

    @Transactional
    public DeployResult execute(UUID serviceId, UUID hostId) {
        return execute(serviceId, hostId, null, null, true);
    }

    @Transactional
    public DeployResult execute(UUID serviceId, UUID hostId, ServiceExposure exposure) {
        return execute(serviceId, hostId, exposure, null, true);
    }

    @Transactional
    public DeployResult execute(
            UUID serviceId, UUID hostId, ServiceExposure exposure, PlacementMode placementMode) {
        return execute(serviceId, hostId, exposure, placementMode, true);
    }

    /** Trusted path used by git webhooks after token validation. */
    @Transactional
    public DeployResult executeTrusted(UUID serviceId, UUID hostId) {
        return execute(serviceId, hostId, null, null, false);
    }

    private DeployResult execute(
            UUID serviceId,
            UUID hostId,
            ServiceExposure exposure,
            PlacementMode placementMode,
            boolean authorize) {
        ServiceUnit service = serviceRepository
                .findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found: " + serviceId));
        Project project = projectRepository
                .findById(service.getProjectId())
                .orElseThrow(() -> new NotFoundException("Project not found: " + service.getProjectId()));
        if (authorize) {
            authorizationService.require(project.getId(), ProjectPermission.DEPLOY);
        }

        ServiceExposure resolvedExposure = exposure == null ? ServiceExposure.PUBLIC : exposure;
        service.updateExposure(resolvedExposure);

        AutopilotPlacementService.PlacementResult placement = autopilotPlacementService.resolveHost(
                hostId, placementMode, project.getId(), service.getName());
        Host host = placement.host();
        UUID resolvedHostId = host.getId();

        if (resolvedExposure == ServiceExposure.PUBLIC) {
            ensurePublicDomainStub(service, project);
        }

        Deployment deployment = deploymentRepository.save(Deployment.create(serviceId, resolvedHostId));

        service.updateStatus(ServiceStatus.DEPLOYING);
        serviceRepository.save(service);
        project.updateStatus(ProjectStatus.DEPLOYING);
        projectRepository.save(project);

        String payload = "{\"deploymentId\":\""
                + deployment.getId()
                + "\",\"serviceId\":\""
                + serviceId
                + "\",\"hostId\":\""
                + resolvedHostId
                + "\",\"exposure\":\""
                + resolvedExposure.name()
                + "\",\"placementMode\":\""
                + placement.effectiveMode().name()
                + "\"}";
        Job job = enqueueJobUseCase.execute(
                new EnqueueJobUseCase.EnqueueJobCommand(JobType.DEPLOY_SERVICE, payload, 3));

        recordAuditUseCase.execute(
                "DEPLOY_SERVICE",
                "deployment",
                deployment.getId(),
                "{\"serviceId\":\""
                        + serviceId
                        + "\",\"hostId\":\""
                        + resolvedHostId
                        + "\",\"exposure\":\""
                        + resolvedExposure.name()
                        + "\",\"placementMode\":\""
                        + placement.effectiveMode().name()
                        + "\",\"placementReason\":\""
                        + escapeJson(placement.reason())
                        + "\",\"jobId\":\""
                        + job.getId()
                        + "\"}");

        return new DeployResult(deployment, job);
    }

    /**
     * Deprecated path: deploy default service of a project (legacy application id = project id).
     */
    @Transactional
    public DeployResult executeForProject(UUID projectId, UUID hostId) {
        return executeForProject(projectId, hostId, null, null);
    }

    @Transactional
    public DeployResult executeForProject(UUID projectId, UUID hostId, ServiceExposure exposure) {
        return executeForProject(projectId, hostId, exposure, null);
    }

    @Transactional
    public DeployResult executeForProject(
            UUID projectId, UUID hostId, ServiceExposure exposure, PlacementMode placementMode) {
        ServiceUnit service = serviceRepository
                .findDefaultByProjectId(projectId)
                .orElseThrow(() -> new NotFoundException("Default service not found for project: " + projectId));
        return execute(service.getId(), hostId, exposure, placementMode);
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void ensurePublicDomainStub(ServiceUnit service, Project project) {
        String hostname = service.getDomain();
        if (hostname == null || hostname.isBlank()) {
            hostname = sanitizeHostnameLabel(service.getName()) + ".atlas.local";
            service.updateDomain(hostname);
        }
        if (!domainRepository.existsByProjectIdAndHostnameIgnoreCase(project.getId(), hostname)) {
            domainRepository.save(Domain.create(project.getId(), hostname, service.getId()));
        }
    }

    static String sanitizeHostnameLabel(String name) {
        String label = name == null ? "app" : name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+", "-");
        label = label.replaceAll("(^-|-$)", "");
        if (label.isBlank()) {
            return "app";
        }
        if (label.length() > 63) {
            return label.substring(0, 63).replaceAll("-$", "");
        }
        return label;
    }

    public record DeployResult(Deployment deployment, Job job) {}
}
