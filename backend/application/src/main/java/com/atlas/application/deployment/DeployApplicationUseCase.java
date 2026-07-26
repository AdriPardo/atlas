package com.atlas.application.deployment;

import com.atlas.application.job.EnqueueJobUseCase;
import com.atlas.application.port.out.ApplicationRepositoryPort;
import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.domain.application.Application;
import com.atlas.domain.application.ApplicationStatus;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobType;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeployApplicationUseCase {

    private final ApplicationRepositoryPort applicationRepository;
    private final HostRepositoryPort hostRepository;
    private final DeploymentRepositoryPort deploymentRepository;
    private final EnqueueJobUseCase enqueueJobUseCase;

    @Transactional
    public DeployResult execute(UUID applicationId, UUID hostId) {
        Application application = applicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found: " + applicationId));
        if (hostRepository.findById(hostId).isEmpty()) {
            throw new NotFoundException("Host not found: " + hostId);
        }

        Deployment deployment = deploymentRepository.save(Deployment.create(applicationId, hostId));

        application.update(
                application.getName(),
                application.getDescription(),
                application.getRepositoryUrl(),
                application.getBranch(),
                application.getComposePath(),
                application.getDomain(),
                ApplicationStatus.DEPLOYING);
        applicationRepository.save(application);

        String payload = "{\"deploymentId\":\""
                + deployment.getId()
                + "\",\"applicationId\":\""
                + applicationId
                + "\",\"hostId\":\""
                + hostId
                + "\"}";
        Job job = enqueueJobUseCase.execute(
                new EnqueueJobUseCase.EnqueueJobCommand(JobType.DEPLOY_SERVICE, payload, 3));

        return new DeployResult(deployment, job);
    }

    public record DeployResult(Deployment deployment, Job job) {}
}
