package com.atlas.application.service;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.DeploymentRepositoryPort;
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
public class DeleteServiceUseCase {

    private final ServiceRepositoryPort serviceRepository;
    private final DeploymentRepositoryPort deploymentRepository;
    private final ProjectAuthorizationService authorizationService;

    @Transactional
    public void execute(UUID id) {
        ServiceUnit service = serviceRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Service not found: " + id));
        authorizationService.require(service.getProjectId(), ProjectPermission.WRITE);
        if (deploymentRepository.existsByServiceId(id)) {
            throw new ConflictException("Cannot delete service with existing deployments");
        }
        serviceRepository.deleteById(id);
    }
}
