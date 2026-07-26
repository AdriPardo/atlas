package com.atlas.application.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.job.EnqueueJobUseCase;
import com.atlas.application.port.out.ApplicationRepositoryPort;
import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.domain.application.Application;
import com.atlas.domain.application.ApplicationStatus;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.deployment.DeploymentStatus;
import com.atlas.domain.host.Host;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobType;
import com.atlas.domain.shared.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeployApplicationUseCaseTest {

    @Mock
    private ApplicationRepositoryPort applicationRepository;

    @Mock
    private HostRepositoryPort hostRepository;

    @Mock
    private DeploymentRepositoryPort deploymentRepository;

    @Mock
    private EnqueueJobUseCase enqueueJobUseCase;

    @InjectMocks
    private DeployApplicationUseCase useCase;

    @Test
    void createsPendingDeploymentAndEnqueuesJob() {
        UUID appId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        Application app = Application.create(
                "demo", "d", "https://git.example/demo.git", "main", "./docker-compose.yml", "");
        Host host = Host.create("local", "127.0.0.1", "linux", "", false, null, null, null, null);
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(hostRepository.findById(hostId)).thenReturn(Optional.of(host));
        when(deploymentRepository.save(any(Deployment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        Job job = Job.enqueue(JobType.DEPLOY_SERVICE, "{}", 3);
        when(enqueueJobUseCase.execute(any())).thenReturn(job);

        DeployApplicationUseCase.DeployResult result = useCase.execute(appId, hostId);

        assertEquals(DeploymentStatus.PENDING, result.deployment().getStatus());
        assertEquals(hostId, result.deployment().getHostId());
        assertEquals(ApplicationStatus.DEPLOYING, app.getStatus());
        assertEquals(job.getId(), result.job().getId());
        verify(enqueueJobUseCase).execute(any());
    }

    @Test
    void failsWhenApplicationMissing() {
        UUID appId = UUID.randomUUID();
        when(applicationRepository.findById(appId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> useCase.execute(appId, UUID.randomUUID()));
    }

    @Test
    void failsWhenHostMissing() {
        UUID appId = UUID.randomUUID();
        Application app = Application.create(
                "demo", "d", "https://git.example/demo.git", "main", "./docker-compose.yml", "");
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(hostRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> useCase.execute(appId, UUID.randomUUID()));
    }
}
