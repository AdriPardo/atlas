package com.atlas.application.project;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.shared.ConflictException;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteProjectUseCase {

    private final ProjectRepositoryPort projectRepository;
    private final DeploymentRepositoryPort deploymentRepository;
    private final ProjectAuthorizationService authorizationService;

    @Transactional
    public void execute(UUID id) {
        authorizationService.require(id, ProjectPermission.WRITE);
        if (projectRepository.findById(id).isEmpty()) {
            throw new NotFoundException("Project not found: " + id);
        }
        if (deploymentRepository.existsByProjectId(id)) {
            throw new ConflictException("Cannot delete project with existing deployments");
        }
        projectRepository.deleteById(id);
    }
}
