package com.atlas.application.deployment;

import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetDeploymentUseCase {

    private final DeploymentRepositoryPort deploymentRepository;

    @Transactional(readOnly = true)
    public Deployment execute(UUID id) {
        return deploymentRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Deployment not found: " + id));
    }
}
