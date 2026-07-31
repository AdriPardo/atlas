package com.atlas.application.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.observability.EvaluateProductAlertsUseCase;
import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.JobRepositoryPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.deployment.DeploymentStatus;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobStatus;
import com.atlas.domain.job.JobType;
import com.atlas.domain.observability.AlertEventType;
import com.atlas.domain.project.Project;
import com.atlas.domain.project.ProjectStatus;
import com.atlas.domain.service.ServiceStatus;
import com.atlas.domain.service.ServiceUnit;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecoverStaleJobsUseCaseTest {

    @Mock
    private JobRepositoryPort jobRepository;

    @Mock
    private DeploymentRepositoryPort deploymentRepository;

    @Mock
    private ServiceRepositoryPort serviceRepository;

    @Mock
    private ProjectRepositoryPort projectRepository;

    @Mock
    private EvaluateProductAlertsUseCase evaluateProductAlertsUseCase;

    @Mock
    private com.atlas.application.port.out.BillingMeterPort billingMeter;

    @InjectMocks
    private RecoverStaleJobsUseCase useCase;

    @Test
    void marksStaleRunningJobFailedAndClearsLease() {
        Instant lockedAt = Instant.now().minus(Duration.ofHours(2));
        Job stale = Job.rehydrate(
                UUID.randomUUID(),
                JobType.SYNC_HOST,
                "{\"hostId\":\"" + UUID.randomUUID() + "\"}",
                JobStatus.RUNNING,
                1,
                3,
                Instant.now().minus(Duration.ofHours(2)),
                lockedAt,
                "dead-worker",
                lockedAt,
                null,
                null,
                Instant.now().minus(Duration.ofHours(2)),
                lockedAt);
        when(jobRepository.findAndLockStaleRunning(any(Instant.class), anyInt())).thenReturn(List.of(stale));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        int recovered = useCase.execute(Duration.ofMinutes(30));

        assertEquals(1, recovered);
        ArgumentCaptor<Job> saved = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(saved.capture());
        assertEquals(JobStatus.FAILED, saved.getValue().getStatus());
        assertNull(saved.getValue().getLockedBy());
        assertNull(saved.getValue().getLockedAt());
        assertTrue(saved.getValue().getLastError().contains(RecoverStaleJobsUseCase.STALE_ERROR_PREFIX));
        verify(evaluateProductAlertsUseCase)
                .execute(eq(AlertEventType.JOB_FAILED), isNull(), any(), eq("job"), eq(stale.getId()));
        verify(deploymentRepository, never()).findById(any());
    }

    @Test
    void cascadesDeployServiceFailureToDeploymentServiceAndProject() {
        Project project = Project.create("reelpath", "");
        project.updateStatus(ProjectStatus.DEPLOYING);
        ServiceUnit service = ServiceUnit.createDefault(
                project.getId(), "https://git.example/r.git", "main", "./docker-compose.yml", "r.local");
        service.updateStatus(ServiceStatus.DEPLOYING);
        Deployment deployment = Deployment.create(service.getId(), UUID.randomUUID());
        deployment.markRunning();

        Instant lockedAt = Instant.now().minus(Duration.ofHours(1));
        String payload = "{\"deploymentId\":\""
                + deployment.getId()
                + "\",\"serviceId\":\""
                + service.getId()
                + "\"}";
        Job stale = Job.rehydrate(
                UUID.randomUUID(),
                JobType.DEPLOY_SERVICE,
                payload,
                JobStatus.RUNNING,
                1,
                3,
                lockedAt,
                lockedAt,
                "crashed-worker",
                lockedAt,
                null,
                null,
                lockedAt,
                lockedAt);

        when(jobRepository.findAndLockStaleRunning(any(Instant.class), anyInt())).thenReturn(List.of(stale));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deploymentRepository.findById(deployment.getId())).thenReturn(Optional.of(deployment));
        when(deploymentRepository.save(any(Deployment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(serviceRepository.findById(service.getId())).thenReturn(Optional.of(service));
        when(serviceRepository.save(any(ServiceUnit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        int recovered = useCase.execute(Duration.ofSeconds(60));

        assertEquals(1, recovered);
        assertEquals(DeploymentStatus.FAILED, deployment.getStatus());
        assertEquals(ServiceStatus.FAILED, service.getStatus());
        assertEquals(ProjectStatus.FAILED, project.getStatus());
        verify(evaluateProductAlertsUseCase)
                .execute(eq(AlertEventType.DEPLOY_FAILED), isNull(), any(), eq("deployment"), eq(deployment.getId()));
    }

    @Test
    void extractDeploymentIdParsesPayload() {
        UUID id = UUID.randomUUID();
        assertEquals(
                Optional.of(id),
                RecoverStaleJobsUseCase.extractDeploymentId("{\"deploymentId\":\"" + id + "\"}"));
        assertTrue(RecoverStaleJobsUseCase.extractDeploymentId("{}").isEmpty());
    }

    @Test
    void returnsZeroWhenNoStaleJobs() {
        when(jobRepository.findAndLockStaleRunning(any(Instant.class), anyInt())).thenReturn(List.of());
        assertEquals(0, useCase.execute(Duration.ofMinutes(30)));
        verify(jobRepository, never()).save(any());
    }
}
