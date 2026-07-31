package com.atlas.application.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.job.EnqueueJobUseCase;
import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.DomainRepositoryPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.deployment.DeploymentStatus;
import com.atlas.domain.deployment.PlacementMode;
import com.atlas.domain.host.ConnectionType;
import com.atlas.domain.host.Host;
import com.atlas.domain.job.Job;
import com.atlas.domain.job.JobType;
import com.atlas.domain.networking.Domain;
import com.atlas.domain.project.Project;
import com.atlas.domain.service.ServiceExposure;
import com.atlas.domain.service.ServiceStatus;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.domain.shared.NotFoundException;
import com.atlas.domain.runtime.RuntimeCapability;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private DeploymentRepositoryPort deploymentRepository;

    @Mock
    private DomainRepositoryPort domainRepository;

    @Mock
    private AutopilotPlacementService autopilotPlacementService;

    @Mock
    private ResolvePlacementRuntimeCapabilityUseCase resolvePlacementRuntimeCapability;

    @Mock
    private EnqueueJobUseCase enqueueJobUseCase;

    @Mock
    private ProjectAuthorizationService authorizationService;

    @Mock
    private RecordAuditUseCase recordAuditUseCase;

    @Mock
    private com.atlas.application.port.out.BillingMeterPort billingMeter;

    @InjectMocks
    private DeployServiceUseCase useCase;

    private static AutopilotPlacementService.PlacementResult placementOf(Host host) {
        return new AutopilotPlacementService.PlacementResult(
                host, PlacementMode.SHARED, "test", null);
    }

    @Test
    void createsPendingDeploymentAndEnqueuesJob() {
        Project project = Project.create("demo", "d");
        ServiceUnit service = ServiceUnit.createDefault(
                project.getId(), "https://git.example/demo.git", "main", "./docker-compose.yml", "demo.atlas.local");
        UUID hostId = UUID.randomUUID();
        Host host = Host.create("local", "127.0.0.1", "linux", "", false, null, null, null, null);
        when(serviceRepository.findById(service.getId())).thenReturn(Optional.of(service));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(autopilotPlacementService.resolveHost(
                        eq(hostId),
                        nullable(PlacementMode.class),
                        eq(project.getId()),
                        any(),
                        nullable(RuntimeCapability.class)))
                .thenReturn(placementOf(host));
        when(domainRepository.existsByProjectIdAndHostnameIgnoreCase(project.getId(), "demo.atlas.local"))
                .thenReturn(false);
        when(domainRepository.save(any(Domain.class))).thenAnswer(inv -> inv.getArgument(0));
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
        assertEquals(host.getId(), result.deployment().getHostId());
        assertEquals(service.getId(), result.deployment().getServiceId());
        assertEquals(ServiceStatus.DEPLOYING, service.getStatus());
        assertEquals(ServiceExposure.PUBLIC, service.getExposure());
        assertEquals(job.getId(), result.job().getId());
        verify(domainRepository).save(any(Domain.class));
        verify(enqueueJobUseCase).execute(any());
        verify(authorizationService).require(project.getId(), ProjectPermission.DEPLOY);
    }

    @Test
    void autoPlacesWhenHostIdOmitted() {
        Project project = Project.create("demo", "d");
        ServiceUnit service = ServiceUnit.createDefault(
                project.getId(), "https://git.example/demo.git", "main", "./docker-compose.yml", "");
        Host host = Host.create(
                AutopilotPlacementService.DEFAULT_LOCAL_HOSTNAME,
                "127.0.0.1",
                "linux",
                "",
                true,
                ConnectionType.LOCAL,
                null,
                22,
                null);
        when(serviceRepository.findById(service.getId())).thenReturn(Optional.of(service));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(resolvePlacementRuntimeCapability.execute(service)).thenReturn(RuntimeCapability.COMPOSE);
        when(autopilotPlacementService.resolveHost(
                        nullable(UUID.class),
                        nullable(PlacementMode.class),
                        eq(project.getId()),
                        any(),
                        eq(RuntimeCapability.COMPOSE)))
                .thenReturn(placementOf(host));
        when(domainRepository.existsByProjectIdAndHostnameIgnoreCase(eq(project.getId()), anyString()))
                .thenReturn(false);
        when(domainRepository.save(any(Domain.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deploymentRepository.save(any(Deployment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(serviceRepository.save(any(ServiceUnit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
        Job job = Job.enqueue(JobType.DEPLOY_SERVICE, "{}", 3);
        when(enqueueJobUseCase.execute(any())).thenReturn(job);
        doNothing().when(authorizationService).require(eq(project.getId()), eq(ProjectPermission.DEPLOY));
        when(recordAuditUseCase.execute(anyString(), anyString(), any(), anyString()))
                .thenReturn(com.atlas.domain.audit.AuditEntry.record(
                        UUID.randomUUID(), "admin", "DEPLOY_SERVICE", "deployment", UUID.randomUUID(), "{}"));

        DeployServiceUseCase.DeployResult result = useCase.execute(service.getId(), null, ServiceExposure.PUBLIC);

        assertEquals(host.getId(), result.deployment().getHostId());
        assertEquals("default.atlas.local", service.getDomain());
        ArgumentCaptor<Domain> domainCaptor = ArgumentCaptor.forClass(Domain.class);
        verify(domainRepository).save(domainCaptor.capture());
        assertEquals("default.atlas.local", domainCaptor.getValue().getHostname());
        verify(resolvePlacementRuntimeCapability).execute(service);
    }

    @Test
    void autoPlacesWithPodmanCapabilityFromManifestPeek() {
        Project project = Project.create("demo", "d");
        ServiceUnit service = ServiceUnit.createDefault(
                project.getId(), "https://git.example/demo.git", "main", "", "");
        Host podmanHost = Host.create(
                "podman-box",
                "10.0.0.9",
                "linux",
                "",
                true,
                ConnectionType.LOCAL,
                null,
                22,
                null,
                java.util.Set.of(RuntimeCapability.PODMAN));
        when(serviceRepository.findById(service.getId())).thenReturn(Optional.of(service));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(resolvePlacementRuntimeCapability.execute(service)).thenReturn(RuntimeCapability.PODMAN);
        when(autopilotPlacementService.resolveHost(
                        nullable(UUID.class),
                        nullable(PlacementMode.class),
                        eq(project.getId()),
                        any(),
                        eq(RuntimeCapability.PODMAN)))
                .thenReturn(placementOf(podmanHost));
        when(domainRepository.existsByProjectIdAndHostnameIgnoreCase(eq(project.getId()), anyString()))
                .thenReturn(false);
        when(domainRepository.save(any(Domain.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deploymentRepository.save(any(Deployment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(serviceRepository.save(any(ServiceUnit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
        Job job = Job.enqueue(JobType.DEPLOY_SERVICE, "{}", 3);
        when(enqueueJobUseCase.execute(any())).thenReturn(job);
        doNothing().when(authorizationService).require(eq(project.getId()), eq(ProjectPermission.DEPLOY));
        when(recordAuditUseCase.execute(anyString(), anyString(), any(), anyString()))
                .thenReturn(com.atlas.domain.audit.AuditEntry.record(
                        UUID.randomUUID(), "admin", "DEPLOY_SERVICE", "deployment", UUID.randomUUID(), "{}"));

        DeployServiceUseCase.DeployResult result =
                useCase.execute(service.getId(), null, ServiceExposure.PUBLIC);

        assertEquals(podmanHost.getId(), result.deployment().getHostId());
        verify(resolvePlacementRuntimeCapability).execute(service);
        verify(autopilotPlacementService)
                .resolveHost(
                        nullable(UUID.class),
                        nullable(PlacementMode.class),
                        eq(project.getId()),
                        any(),
                        eq(RuntimeCapability.PODMAN));
    }

    @Test
    void internalExposureSkipsPublicDomainStub() {
        Project project = Project.create("demo", "d");
        ServiceUnit service = ServiceUnit.createDefault(
                project.getId(), "https://git.example/demo.git", "main", "./docker-compose.yml", "");
        Host host = Host.create("local", "127.0.0.1", "linux", "", true, ConnectionType.LOCAL, null, 22, null);
        when(serviceRepository.findById(service.getId())).thenReturn(Optional.of(service));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(resolvePlacementRuntimeCapability.execute(service)).thenReturn(RuntimeCapability.COMPOSE);
        when(autopilotPlacementService.resolveHost(
                        nullable(UUID.class),
                        nullable(PlacementMode.class),
                        eq(project.getId()),
                        any(),
                        eq(RuntimeCapability.COMPOSE)))
                .thenReturn(placementOf(host));
        when(deploymentRepository.save(any(Deployment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(serviceRepository.save(any(ServiceUnit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
        Job job = Job.enqueue(JobType.DEPLOY_SERVICE, "{}", 3);
        when(enqueueJobUseCase.execute(any())).thenReturn(job);
        doNothing().when(authorizationService).require(eq(project.getId()), eq(ProjectPermission.DEPLOY));
        when(recordAuditUseCase.execute(anyString(), anyString(), any(), anyString()))
                .thenReturn(com.atlas.domain.audit.AuditEntry.record(
                        UUID.randomUUID(), "admin", "DEPLOY_SERVICE", "deployment", UUID.randomUUID(), "{}"));

        useCase.execute(service.getId(), null, ServiceExposure.INTERNAL);

        assertEquals(ServiceExposure.INTERNAL, service.getExposure());
        assertTrue(service.getDomain().isBlank());
        verify(domainRepository, never()).save(any());
    }

    @Test
    void failsWhenServiceMissing() {
        UUID serviceId = UUID.randomUUID();
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> useCase.execute(serviceId, UUID.randomUUID()));
    }
}
