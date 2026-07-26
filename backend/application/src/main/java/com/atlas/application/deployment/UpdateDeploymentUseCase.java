package com.atlas.application.deployment;

import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.deployment.DeploymentStatus;
import com.atlas.domain.shared.NotFoundException;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateDeploymentUseCase {

    private final DeploymentRepositoryPort deploymentRepository;

    @Transactional
    public Deployment execute(UUID id, UpdateDeploymentCommand command) {
        Deployment deployment = deploymentRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Deployment not found: " + id));
        deployment.updateStatus(command.status(), command.startedAt(), command.finishedAt(), command.logs());
        return deploymentRepository.save(deployment);
    }

    public record UpdateDeploymentCommand(
            DeploymentStatus status, Instant startedAt, Instant finishedAt, String logs) {}
}
