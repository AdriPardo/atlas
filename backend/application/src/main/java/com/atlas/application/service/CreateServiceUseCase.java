package com.atlas.application.service;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.domain.shared.ConflictException;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateServiceUseCase {

    private final ProjectRepositoryPort projectRepository;
    private final ServiceRepositoryPort serviceRepository;
    private final ProjectAuthorizationService authorizationService;

    @Transactional
    public ServiceUnit execute(UUID projectId, CreateServiceCommand command) {
        authorizationService.require(projectId, ProjectPermission.WRITE);
        if (projectRepository.findById(projectId).isEmpty()) {
            throw new NotFoundException("Project not found: " + projectId);
        }
        String name = command.name() == null || command.name().isBlank()
                ? ServiceUnit.DEFAULT_NAME
                : command.name();
        if (serviceRepository.existsByProjectIdAndName(projectId, name)) {
            throw new ConflictException("Service name already exists in project: " + name);
        }
        return serviceRepository.save(ServiceUnit.create(
                projectId,
                name,
                command.repositoryUrl(),
                command.branch(),
                command.composePath(),
                command.domain(),
                command.environment()));
    }

    public record CreateServiceCommand(
            String name,
            String repositoryUrl,
            String branch,
            String composePath,
            String domain,
            String environment) {}
}
