package com.atlas.application.pipeline;

import com.atlas.application.deployment.DeployServiceUseCase;
import com.atlas.application.port.out.PipelineRepositoryPort;
import com.atlas.application.port.out.PipelineRunRepositoryPort;
import com.atlas.domain.pipeline.Pipeline;
import com.atlas.domain.pipeline.PipelineRun;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RunPipelineUseCase {

    private final PipelineRepositoryPort pipelineRepository;
    private final PipelineRunRepositoryPort pipelineRunRepository;
    private final DeployServiceUseCase deployServiceUseCase;

    @Transactional
    public PipelineRun execute(UUID pipelineId, String triggeredBy) {
        Pipeline pipeline = pipelineRepository
                .findById(pipelineId)
                .orElseThrow(() -> new NotFoundException("Pipeline not found: " + pipelineId));

        PipelineRun run = pipelineRunRepository.save(PipelineRun.start(pipelineId, triggeredBy));
        try {
            DeployServiceUseCase.DeployResult result =
                    deployServiceUseCase.execute(pipeline.getServiceId(), pipeline.getHostId());
            run.markRunning(result.deployment().getId(), result.job().getId());
            return pipelineRunRepository.save(run);
        } catch (RuntimeException ex) {
            run.markFailed(ex.getMessage());
            pipelineRunRepository.save(run);
            throw ex;
        }
    }
}
