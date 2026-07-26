package com.atlas.application.deployment;

import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteDeploymentUseCase {

    private final DeploymentRepositoryPort deploymentRepository;

    @Transactional
    public void execute(UUID id) {
        if (deploymentRepository.findById(id).isEmpty()) {
            throw new NotFoundException("Deployment not found: " + id);
        }
        deploymentRepository.deleteById(id);
    }
}
