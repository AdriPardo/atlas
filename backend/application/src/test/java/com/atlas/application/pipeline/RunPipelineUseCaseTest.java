package com.atlas.application.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.deployment.DeployServiceUseCase;
import com.atlas.application.port.out.PipelineRepositoryPort;
import com.atlas.application.port.out.PipelineRunRepositoryPort;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobType;
import com.atlas.domain.pipeline.Pipeline;
import com.atlas.domain.pipeline.PipelineRun;
import com.atlas.domain.pipeline.PipelineRunStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RunPipelineUseCaseTest {

    @Mock
    private PipelineRepositoryPort pipelineRepository;

    @Mock
    private PipelineRunRepositoryPort pipelineRunRepository;

    @Mock
    private DeployServiceUseCase deployServiceUseCase;

    @InjectMocks
    private RunPipelineUseCase useCase;

    @Test
    void enqueuesDeployAndMarksRunRunning() {
        UUID projectId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        Pipeline pipeline = Pipeline.create(projectId, "deploy-prod", serviceId, hostId);
        when(pipelineRepository.findById(pipeline.getId())).thenReturn(Optional.of(pipeline));
        when(pipelineRunRepository.save(any(PipelineRun.class))).thenAnswer(inv -> inv.getArgument(0));

        Deployment deployment = Deployment.create(serviceId, hostId);
        Job job = Job.enqueue(JobType.DEPLOY_SERVICE, "{}", 3);
        when(deployServiceUseCase.execute(serviceId, hostId))
                .thenReturn(new DeployServiceUseCase.DeployResult(deployment, job));

        PipelineRun run = useCase.execute(pipeline.getId(), "manual");

        assertEquals(PipelineRunStatus.RUNNING, run.getStatus());
        assertEquals(deployment.getId(), run.getDeploymentId());
        assertEquals(job.getId(), run.getJobId());
        assertNotNull(run.getStartedAt());
        verify(deployServiceUseCase).execute(serviceId, hostId);
    }
}
