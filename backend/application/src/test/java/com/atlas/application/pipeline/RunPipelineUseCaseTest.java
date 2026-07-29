package com.atlas.application.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.deployment.DeployServiceUseCase;
import com.atlas.application.port.out.PipelineRepositoryPort;
import com.atlas.application.port.out.PipelineRunRepositoryPort;
import com.atlas.domain.access.ProjectPermission;
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

    @Mock
    private ProjectAuthorizationService authorizationService;

    @Mock
    private RecordAuditUseCase recordAuditUseCase;

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
        doNothing().when(authorizationService).require(eq(projectId), eq(ProjectPermission.DEPLOY));

        Deployment deployment = Deployment.create(serviceId, hostId);
        Job job = Job.enqueue(JobType.DEPLOY_SERVICE, "{}", 3);
        when(deployServiceUseCase.execute(serviceId, hostId))
                .thenReturn(new DeployServiceUseCase.DeployResult(deployment, job));
        when(recordAuditUseCase.execute(anyString(), anyString(), any(), anyString()))
                .thenReturn(com.atlas.domain.audit.AuditEntry.record(
                        UUID.randomUUID(), "admin", "PIPELINE_RUN", "pipeline_run", UUID.randomUUID(), "{}"));

        PipelineRun run = useCase.execute(pipeline.getId(), "manual");

        assertEquals(PipelineRunStatus.RUNNING, run.getStatus());
        assertEquals(deployment.getId(), run.getDeploymentId());
        assertEquals(job.getId(), run.getJobId());
        assertNotNull(run.getStartedAt());
        verify(deployServiceUseCase).execute(serviceId, hostId);
        verify(authorizationService).require(projectId, ProjectPermission.DEPLOY);
    }

    @Test
    void passesNullHostIdForAutopilotPlacement() {
        UUID projectId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID resolvedHostId = UUID.randomUUID();
        Pipeline pipeline = Pipeline.create(projectId, "auto-deploy", serviceId, null);
        assertNull(pipeline.getHostId());

        when(pipelineRepository.findById(pipeline.getId())).thenReturn(Optional.of(pipeline));
        when(pipelineRunRepository.save(any(PipelineRun.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(authorizationService).require(eq(projectId), eq(ProjectPermission.DEPLOY));

        Deployment deployment = Deployment.create(serviceId, resolvedHostId);
        Job job = Job.enqueue(JobType.DEPLOY_SERVICE, "{}", 3);
        when(deployServiceUseCase.execute(eq(serviceId), isNull()))
                .thenReturn(new DeployServiceUseCase.DeployResult(deployment, job));
        when(recordAuditUseCase.execute(anyString(), anyString(), any(), anyString()))
                .thenReturn(com.atlas.domain.audit.AuditEntry.record(
                        UUID.randomUUID(), "admin", "PIPELINE_RUN", "pipeline_run", UUID.randomUUID(), "{}"));

        PipelineRun run = useCase.execute(pipeline.getId(), "manual");

        assertEquals(PipelineRunStatus.RUNNING, run.getStatus());
        verify(deployServiceUseCase).execute(serviceId, null);
    }

    @Test
    void trustedPathPassesNullHostId() {
        UUID projectId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID resolvedHostId = UUID.randomUUID();
        Pipeline pipeline = Pipeline.create(projectId, "auto-deploy", serviceId, null);

        when(pipelineRepository.findById(pipeline.getId())).thenReturn(Optional.of(pipeline));
        when(pipelineRunRepository.save(any(PipelineRun.class))).thenAnswer(inv -> inv.getArgument(0));

        Deployment deployment = Deployment.create(serviceId, resolvedHostId);
        Job job = Job.enqueue(JobType.DEPLOY_SERVICE, "{}", 3);
        when(deployServiceUseCase.executeTrusted(eq(serviceId), isNull()))
                .thenReturn(new DeployServiceUseCase.DeployResult(deployment, job));
        when(recordAuditUseCase.execute(anyString(), anyString(), any(), anyString()))
                .thenReturn(com.atlas.domain.audit.AuditEntry.record(
                        UUID.randomUUID(), "admin", "PIPELINE_RUN", "pipeline_run", UUID.randomUUID(), "{}"));

        PipelineRun run = useCase.executeTrusted(pipeline.getId(), "webhook");

        assertEquals(PipelineRunStatus.RUNNING, run.getStatus());
        verify(deployServiceUseCase).executeTrusted(serviceId, null);
    }
}
