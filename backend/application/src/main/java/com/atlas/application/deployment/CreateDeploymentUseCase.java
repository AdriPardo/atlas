package com.atlas.application.deployment;

import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateDeploymentUseCase {

    private final DeploymentRepositoryPort deploymentRepository;
    private final ServiceRepositoryPort serviceRepository;
    private final HostRepositoryPort hostRepository;

    @Transactional
    public Deployment execute(CreateDeploymentCommand command) {
        UUID serviceId = command.serviceId();
        if (serviceId == null && command.applicationId() != null) {
            // Deprecated: applicationId means project id → resolve default service
            serviceId = serviceRepository
                    .findDefaultByProjectId(command.applicationId())
                    .orElseThrow(() -> new NotFoundException(
                            "Default service not found for project: " + command.applicationId()))
                    .getId();
        }
        if (serviceId == null) {
            throw new NotFoundException("serviceId is required");
        }
        if (serviceRepository.findById(serviceId).isEmpty()) {
            throw new NotFoundException("Service not found: " + serviceId);
        }
        if (hostRepository.findById(command.hostId()).isEmpty()) {
            throw new NotFoundException("Host not found: " + command.hostId());
        }
        Deployment deployment = Deployment.create(serviceId, command.hostId());
        return deploymentRepository.save(deployment);
    }

    public record CreateDeploymentCommand(UUID serviceId, UUID applicationId, UUID hostId) {
        public CreateDeploymentCommand(UUID serviceId, UUID hostId) {
            this(serviceId, null, hostId);
        }
    }
}
