package com.atlas.application.host;

import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.domain.shared.ConflictException;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteHostUseCase {

    private final HostRepositoryPort hostRepository;
    private final DeploymentRepositoryPort deploymentRepository;

    @Transactional
    public void execute(UUID id) {
        if (hostRepository.findById(id).isEmpty()) {
            throw new NotFoundException("Host not found: " + id);
        }
        if (deploymentRepository.existsByHostId(id)) {
            throw new ConflictException("Cannot delete host with existing deployments");
        }
        hostRepository.deleteById(id);
    }
}
