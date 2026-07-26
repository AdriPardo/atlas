package com.atlas.application.pipeline;

import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.application.port.out.PipelineRepositoryPort;
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
public class UpdatePipelineUseCase {

    private final PipelineRepositoryPort pipelineRepository;
    private final ServiceRepositoryPort serviceRepository;
    private final HostRepositoryPort hostRepository;

    @Transactional
    public Pipeline execute(UUID id, UpdatePipelineCommand command) {
        Pipeline pipeline = pipelineRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Pipeline not found: " + id));
        ServiceUnit service = serviceRepository
                .findById(command.serviceId())
                .orElseThrow(() -> new NotFoundException("Service not found: " + command.serviceId()));
        if (!service.getProjectId().equals(pipeline.getProjectId())) {
            throw new DomainException("Service does not belong to pipeline project");
        }
        if (hostRepository.findById(command.hostId()).isEmpty()) {
            throw new NotFoundException("Host not found: " + command.hostId());
        }
        if (pipelineRepository.existsByProjectIdAndNameAndIdNot(
                pipeline.getProjectId(), command.name().trim(), id)) {
            throw new ConflictException("Pipeline name already exists in project");
        }
        pipeline.update(command.name(), command.serviceId(), command.hostId());
        return pipelineRepository.save(pipeline);
    }

    public record UpdatePipelineCommand(String name, UUID serviceId, UUID hostId) {}
}
