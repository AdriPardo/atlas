package com.atlas.application.pipeline;

import com.atlas.application.port.out.PipelineRepositoryPort;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeletePipelineUseCase {

    private final PipelineRepositoryPort pipelineRepository;

    @Transactional
    public void execute(UUID id) {
        if (pipelineRepository.findById(id).isEmpty()) {
            throw new NotFoundException("Pipeline not found: " + id);
        }
        pipelineRepository.deleteById(id);
    }
}
