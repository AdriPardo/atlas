package com.atlas.application.deployment;

import com.atlas.application.port.out.ApplicationRepositoryPort;
import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.HostRepositoryPort;
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
    private final ApplicationRepositoryPort applicationRepository;
    private final HostRepositoryPort hostRepository;

    @Transactional
    public Deployment execute(CreateDeploymentCommand command) {
        if (applicationRepository.findById(command.applicationId()).isEmpty()) {
            throw new NotFoundException("Application not found: " + command.applicationId());
        }
        if (hostRepository.findById(command.hostId()).isEmpty()) {
            throw new NotFoundException("Host not found: " + command.hostId());
        }
        Deployment deployment = Deployment.create(command.applicationId(), command.hostId());
        return deploymentRepository.save(deployment);
    }

    public record CreateDeploymentCommand(UUID applicationId, UUID hostId) {}
}
