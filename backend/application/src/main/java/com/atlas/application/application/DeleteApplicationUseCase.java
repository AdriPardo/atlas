package com.atlas.application.application;

import com.atlas.application.port.out.ApplicationRepositoryPort;
import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.domain.shared.ConflictException;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteApplicationUseCase {

    private final ApplicationRepositoryPort applicationRepository;
    private final DeploymentRepositoryPort deploymentRepository;

    @Transactional
    public void execute(UUID id) {
        if (applicationRepository.findById(id).isEmpty()) {
            throw new NotFoundException("Application not found: " + id);
        }
        if (deploymentRepository.existsByProjectId(id)) {
            throw new ConflictException("Cannot delete application with existing deployments");
        }
        applicationRepository.deleteById(id);
    }
}
