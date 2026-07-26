package com.atlas.application.pipeline;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.PipelineRepositoryPort;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.pipeline.Pipeline;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetPipelineUseCase {

    private final PipelineRepositoryPort pipelineRepository;
    private final ProjectAuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public Pipeline execute(UUID id) {
        Pipeline pipeline = pipelineRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Pipeline not found: " + id));
        authorizationService.require(pipeline.getProjectId(), ProjectPermission.READ);
        return pipeline;
    }
}
