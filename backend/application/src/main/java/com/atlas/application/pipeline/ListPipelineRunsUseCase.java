package com.atlas.application.pipeline;

import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.PipelineRunRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.pipeline.PipelineRun;
import com.atlas.domain.pipeline.PipelineRunStatus;
import com.atlas.domain.shared.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListPipelineRunsUseCase {

    private final PipelineRunRepositoryPort pipelineRunRepository;
    private final DeploymentRepositoryPort deploymentRepository;

    @Transactional
    public PageResult<PipelineRun> execute(UUID pipelineId, PageQuery pageQuery) {
        PageResult<PipelineRun> page = pipelineRunRepository.searchByPipelineId(pipelineId, pageQuery);
        List<PipelineRun> synced = new ArrayList<>(page.content().size());
        for (PipelineRun run : page.content()) {
            synced.add(syncIfNeeded(run));
        }
        return PageResult.of(synced, page.page(), page.size(), page.totalElements(), page.sort());
    }

    @Transactional
    public PipelineRun get(UUID runId) {
        PipelineRun run = pipelineRunRepository
                .findById(runId)
                .orElseThrow(() -> new NotFoundException("Pipeline run not found: " + runId));
        return syncIfNeeded(run);
    }

    private PipelineRun syncIfNeeded(PipelineRun run) {
        if (run.getDeploymentId() == null
                || run.getStatus() == PipelineRunStatus.SUCCEEDED
                || run.getStatus() == PipelineRunStatus.FAILED
                || run.getStatus() == PipelineRunStatus.CANCELLED) {
            return run;
        }
        return deploymentRepository
                .findById(run.getDeploymentId())
                .map(deployment -> applySync(run, deployment))
                .orElse(run);
    }

    private PipelineRun applySync(PipelineRun run, Deployment deployment) {
        PipelineRunStatus before = run.getStatus();
        run.syncFromDeployment(deployment.getStatus(), extractErrorHint(deployment.getLogs()));
        if (before != run.getStatus()) {
            return pipelineRunRepository.save(run);
        }
        return run;
    }

    private static String extractErrorHint(String logs) {
        if (logs == null || logs.isBlank()) {
            return "Deployment failed";
        }
        String[] lines = logs.strip().split("\\R");
        return lines[lines.length - 1];
    }
}
