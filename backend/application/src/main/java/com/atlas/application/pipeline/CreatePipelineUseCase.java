package com.atlas.application.pipeline;

import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.application.port.out.PipelineRepositoryPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.domain.pipeline.Pipeline;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.domain.shared.ConflictException;
import com.atlas.domain.shared.DomainException;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreatePipelineUseCase {

    private final PipelineRepositoryPort pipelineRepository;
    private final ProjectRepositoryPort projectRepository;
    private final ServiceRepositoryPort serviceRepository;
    private final HostRepositoryPort hostRepository;

    @Transactional
    public Pipeline execute(CreatePipelineCommand command) {
        if (projectRepository.findById(command.projectId()).isEmpty()) {
            throw new NotFoundException("Project not found: " + command.projectId());
        }
        ServiceUnit service = serviceRepository
                .findById(command.serviceId())
                .orElseThrow(() -> new NotFoundException("Service not found: " + command.serviceId()));
        if (!service.getProjectId().equals(command.projectId())) {
            throw new DomainException("Service does not belong to project");
        }
        if (hostRepository.findById(command.hostId()).isEmpty()) {
            throw new NotFoundException("Host not found: " + command.hostId());
        }
        if (pipelineRepository.existsByProjectIdAndName(command.projectId(), command.name().trim())) {
            throw new ConflictException("Pipeline name already exists in project");
        }
        return pipelineRepository.save(
                Pipeline.create(command.projectId(), command.name(), command.serviceId(), command.hostId()));
    }

    public record CreatePipelineCommand(UUID projectId, String name, UUID serviceId, UUID hostId) {}
}
