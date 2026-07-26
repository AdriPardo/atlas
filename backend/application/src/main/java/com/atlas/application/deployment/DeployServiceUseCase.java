package com.atlas.application.deployment;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.job.EnqueueJobUseCase;
import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobType;
import com.atlas.domain.project.Project;
import com.atlas.domain.project.ProjectStatus;
import com.atlas.domain.service.ServiceStatus;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeployServiceUseCase {

    private final ServiceRepositoryPort serviceRepository;
    private final ProjectRepositoryPort projectRepository;
    private final HostRepositoryPort hostRepository;
    private final DeploymentRepositoryPort deploymentRepository;
    private final EnqueueJobUseCase enqueueJobUseCase;
    private final ProjectAuthorizationService authorizationService;
    private final RecordAuditUseCase recordAuditUseCase;

    @Transactional
    public DeployResult execute(UUID serviceId, UUID hostId) {
        return execute(serviceId, hostId, true);
    }

    /** Trusted path used by git webhooks after token validation. */
    @Transactional
    public DeployResult executeTrusted(UUID serviceId, UUID hostId) {
        return execute(serviceId, hostId, false);
    }

    private DeployResult execute(UUID serviceId, UUID hostId, boolean authorize) {
        ServiceUnit service = serviceRepository
                .findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found: " + serviceId));
        Project project = projectRepository
                .findById(service.getProjectId())
                .orElseThrow(() -> new NotFoundException("Project not found: " + service.getProjectId()));
        if (authorize) {
            authorizationService.require(project.getId(), ProjectPermission.DEPLOY);
        }
        if (hostRepository.findById(hostId).isEmpty()) {
            throw new NotFoundException("Host not found: " + hostId);
        }

        Deployment deployment = deploymentRepository.save(Deployment.create(serviceId, hostId));

        service.updateStatus(ServiceStatus.DEPLOYING);
        serviceRepository.save(service);
        project.updateStatus(ProjectStatus.DEPLOYING);
        projectRepository.save(project);

        String payload = "{\"deploymentId\":\""
                + deployment.getId()
                + "\",\"serviceId\":\""
                + serviceId
                + "\",\"hostId\":\""
                + hostId
                + "\"}";
        Job job = enqueueJobUseCase.execute(
                new EnqueueJobUseCase.EnqueueJobCommand(JobType.DEPLOY_SERVICE, payload, 3));

        recordAuditUseCase.execute(
                "DEPLOY_SERVICE",
                "deployment",
                deployment.getId(),
                "{\"serviceId\":\"" + serviceId + "\",\"hostId\":\"" + hostId + "\",\"jobId\":\"" + job.getId() + "\"}");

        return new DeployResult(deployment, job);
    }

    /**
     * Deprecated path: deploy default service of a project (legacy application id = project id).
     */
    @Transactional
    public DeployResult executeForProject(UUID projectId, UUID hostId) {
        ServiceUnit service = serviceRepository
                .findDefaultByProjectId(projectId)
                .orElseThrow(() -> new NotFoundException("Default service not found for project: " + projectId));
        return execute(service.getId(), hostId);
    }

    public record DeployResult(Deployment deployment, Job job) {}
}
