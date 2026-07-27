package com.atlas.application.service;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.service.ServiceStatus;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.domain.shared.ConflictException;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateServiceUseCase {

    private final ServiceRepositoryPort serviceRepository;
    private final ProjectAuthorizationService authorizationService;

    @Transactional
    public ServiceUnit execute(UUID id, UpdateServiceCommand command) {
        ServiceUnit service = serviceRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Service not found: " + id));
        authorizationService.require(service.getProjectId(), ProjectPermission.WRITE);
        if (serviceRepository.existsByProjectIdAndNameAndIdNot(
                service.getProjectId(), command.name(), id)) {
            throw new ConflictException("Service name already exists in project: " + command.name());
        }
        service.update(
                command.name(),
                command.repositoryUrl(),
                command.branch(),
                command.composePath(),
                command.domain(),
                command.environment(),
                command.status());
        return serviceRepository.save(service);
    }

    public record UpdateServiceCommand(
            String name,
            String repositoryUrl,
            String branch,
            String composePath,
            String domain,
            String environment,
            ServiceStatus status) {}
}
