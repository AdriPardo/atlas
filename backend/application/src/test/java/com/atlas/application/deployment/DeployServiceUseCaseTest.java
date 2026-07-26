package com.atlas.application.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.job.EnqueueJobUseCase;
import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.deployment.DeploymentStatus;
import com.atlas.domain.host.Host;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobType;
import com.atlas.domain.project.Project;
import com.atlas.domain.service.ServiceStatus;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.domain.shared.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeployServiceUseCaseTest {

    @Mock
    private ServiceRepositoryPort serviceRepository;

    @Mock
    private ProjectRepositoryPort projectRepository;

    @Mock
    private HostRepositoryPort hostRepository;

    @Mock
    private DeploymentRepositoryPort deploymentRepository;

    @Mock
    private EnqueueJobUseCase enqueueJobUseCase;

    @Mock
    private ProjectAuthorizationService authorizationService;

    @Mock
    private RecordAuditUseCase recordAuditUseCase;

    @InjectMocks
    private DeployServiceUseCase useCase;

    @Test
    void createsPendingDeploymentAndEnqueuesJob() {
        Project project = Project.create("demo", "d");
        ServiceUnit service = ServiceUnit.createDefault(
                project.getId(), "https://git.example/demo.git", "main", "./docker-compose.yml", "");
        UUID hostId = UUID.randomUUID();
        Host host = Host.create("local", "127.0.0.1", "linux", "", false, null, null, null, null);
        when(serviceRepository.findById(service.getId())).thenReturn(Optional.of(service));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(hostRepository.findById(hostId)).thenReturn(Optional.of(host));
        when(deploymentRepository.save(any(Deployment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(serviceRepository.save(any(ServiceUnit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
        Job job = Job.enqueue(JobType.DEPLOY_SERVICE, "{}", 3);
        when(enqueueJobUseCase.execute(any())).thenReturn(job);
        doNothing().when(authorizationService).require(eq(project.getId()), eq(ProjectPermission.DEPLOY));
        when(recordAuditUseCase.execute(anyString(), anyString(), any(), anyString()))
                .thenReturn(com.atlas.domain.audit.AuditEntry.record(
                        UUID.randomUUID(), "admin", "DEPLOY_SERVICE", "deployment", UUID.randomUUID(), "{}"));

        DeployServiceUseCase.DeployResult result = useCase.execute(service.getId(), hostId);

        assertEquals(DeploymentStatus.PENDING, result.deployment().getStatus());
        assertEquals(hostId, result.deployment().getHostId());
        assertEquals(service.getId(), result.deployment().getServiceId());
        assertEquals(ServiceStatus.DEPLOYING, service.getStatus());
        assertEquals(job.getId(), result.job().getId());
        verify(enqueueJobUseCase).execute(any());
        verify(authorizationService).require(project.getId(), ProjectPermission.DEPLOY);
    }

    @Test
    void failsWhenServiceMissing() {
        UUID serviceId = UUID.randomUUID();
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> useCase.execute(serviceId, UUID.randomUUID()));
    }
}
