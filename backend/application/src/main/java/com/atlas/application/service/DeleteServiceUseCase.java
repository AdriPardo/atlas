package com.atlas.application.service;

import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
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

    @Transactional
    public void execute(UUID id) {
        if (serviceRepository.findById(id).isEmpty()) {
            throw new NotFoundException("Service not found: " + id);
        }
        if (deploymentRepository.existsByServiceId(id)) {
            throw new ConflictException("Cannot delete service with existing deployments");
        }
        serviceRepository.deleteById(id);
    }
}
