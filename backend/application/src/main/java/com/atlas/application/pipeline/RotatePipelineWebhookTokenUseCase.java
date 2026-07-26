package com.atlas.application.pipeline;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
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
public class RotatePipelineWebhookTokenUseCase {

    private final PipelineRepositoryPort pipelineRepository;
    private final ProjectAuthorizationService authorizationService;
    private final RecordAuditUseCase recordAuditUseCase;

    @Transactional
    public Pipeline execute(UUID pipelineId) {
        Pipeline pipeline = pipelineRepository
                .findById(pipelineId)
                .orElseThrow(() -> new NotFoundException("Pipeline not found: " + pipelineId));
        authorizationService.require(pipeline.getProjectId(), ProjectPermission.WRITE);
        pipeline.rotateWebhookToken();
        Pipeline saved = pipelineRepository.save(pipeline);
        recordAuditUseCase.execute(
                "PIPELINE_WEBHOOK_ROTATE",
                "pipeline",
                saved.getId(),
                "{\"projectId\":\"" + saved.getProjectId() + "\"}");
        return saved;
    }
}
