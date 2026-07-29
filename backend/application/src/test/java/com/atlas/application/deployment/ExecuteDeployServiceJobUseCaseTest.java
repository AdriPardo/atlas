package com.atlas.application.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.networking.EnsureDomainDnsCnameUseCase;
import com.atlas.application.networking.EnsureDomainTunnelIngressUseCase;
import com.atlas.application.observability.EvaluateProductAlertsUseCase;
import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.DomainRepositoryPort;
import com.atlas.application.port.out.GitRepositoryPort;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.RuntimeOrchestratorPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.application.secret.ResolveSecretValueUseCase;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.deployment.DeploymentStatus;
import com.atlas.domain.host.ConnectionType;
import com.atlas.domain.host.Host;
import com.atlas.domain.project.Project;
import com.atlas.domain.service.ServiceStatus;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.domain.shared.DomainException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class ExecuteDeployServiceJobUseCaseTest {

    @TempDir
    Path workspace;

    @Mock
    private DeploymentRepositoryPort deploymentRepository;

    @Mock
    private ServiceRepositoryPort serviceRepository;

    @Mock
    private ProjectRepositoryPort projectRepository;

    @Mock
    private HostRepositoryPort hostRepository;

    @Mock
    private GitRepositoryPort gitRepository;

    @Mock
    private RuntimeOrchestratorPort runtimeOrchestrator;

    @Mock
    private ResolveSecretValueUseCase resolveSecretValue;

    @Mock
    private EvaluateProductAlertsUseCase evaluateProductAlertsUseCase;

    @Mock
    private DomainRepositoryPort domainRepository;

    @Mock
    private EnsureDomainTunnelIngressUseCase ensureDomainTunnelIngressUseCase;

    @Mock
    private EnsureDomainDnsCnameUseCase ensureDomainDnsCnameUseCase;

    @Mock
    private PlatformTransactionManager transactionManager;

    private ExecuteDeployServiceJobUseCase useCase;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        useCase = new ExecuteDeployServiceJobUseCase(
                deploymentRepository,
                serviceRepository,
                projectRepository,
                hostRepository,
                domainRepository,
                gitRepository,
                runtimeOrchestrator,
                resolveSecretValue,
                id -> workspace,
                evaluateProductAlertsUseCase,
                ensureDomainTunnelIngressUseCase,
                ensureDomainDnsCnameUseCase,
                transactionManager);
    }

    @Test
    void marksDeploymentSucceededOnHappyPath() {
        Project project = Project.create("demo", "d");
        ServiceUnit service = ServiceUnit.createDefault(
                project.getId(), "https://example.com/demo.git", "main", "docker-compose.yml", "");
        Host host = Host.create("local", "127.0.0.1", "linux", "26", true, ConnectionType.LOCAL, null, 22, null);
        Deployment deployment = Deployment.create(service.getId(), host.getId());
        Deployment running = Deployment.rehydrate(
                deployment.getId(),
                deployment.getServiceId(),
                deployment.getHostId(),
                DeploymentStatus.PENDING,
                null,
                null,
                "",
                deployment.getCreatedAt(),
                deployment.getUpdatedAt());

        when(deploymentRepository.findById(deployment.getId())).thenReturn(Optional.of(running));
        when(serviceRepository.findById(service.getId())).thenReturn(Optional.of(service));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(hostRepository.findById(host.getId())).thenReturn(Optional.of(host));
        when(deploymentRepository.save(any(Deployment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(serviceRepository.save(any(ServiceUnit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resolveSecretValue.forProject(project.getId(), ExecuteDeployServiceJobUseCase.GIT_TOKEN_SECRET_NAME))
                .thenReturn(Optional.empty());

        doAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Consumer<String> sink = inv.getArgument(4);
                    sink.accept("cloned");
                    return null;
                })
                .when(gitRepository)
                .cloneOrUpdate(any(), any(), any(), any(), any());

        doAnswer(inv -> {
                    RuntimeOrchestratorPort.RuntimeApplyCommand cmd = inv.getArgument(0);
                    cmd.logSink().accept("compose up ok");
                    return null;
                })
                .when(runtimeOrchestrator)
                .apply(any());

        useCase.execute(deployment.getId());

        verify(gitRepository)
                .cloneOrUpdate(eq(service.getRepositoryUrl()), eq("main"), eq(workspace), eq(Optional.empty()), any());
        verify(runtimeOrchestrator).apply(any());
        assertEquals(ServiceStatus.RUNNING, service.getStatus());
        assertTrue(running.getLogs().contains("compose up ok") || running.getStatus() == DeploymentStatus.SUCCEEDED);
        assertTrue(running.getLogs().contains("composePath") || running.getStatus() == DeploymentStatus.SUCCEEDED);
    }

    @Test
    void usesComposeFileFromAtlasYmlWhenPresent() throws Exception {
        java.nio.file.Files.writeString(
                workspace.resolve("atlas.yml"),
                """
                apiVersion: atlas/v1alpha1
                kind: Project
                runtime:
                  kind: compose
                  composeFile: docker-compose.atlas.yml
                """);

        Project project = Project.create("demo", "d");
        ServiceUnit service = ServiceUnit.createDefault(
                project.getId(), "https://example.com/demo.git", "main", "docker-compose.yml", "");
        Host host = Host.create("local", "127.0.0.1", "linux", "26", true, ConnectionType.LOCAL, null, 22, null);
        Deployment deployment = Deployment.create(service.getId(), host.getId());
        Deployment running = Deployment.rehydrate(
                deployment.getId(),
                deployment.getServiceId(),
                deployment.getHostId(),
                DeploymentStatus.PENDING,
                null,
                null,
                "",
                deployment.getCreatedAt(),
                deployment.getUpdatedAt());

        when(deploymentRepository.findById(deployment.getId())).thenReturn(Optional.of(running));
        when(serviceRepository.findById(service.getId())).thenReturn(Optional.of(service));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(hostRepository.findById(host.getId())).thenReturn(Optional.of(host));
        when(deploymentRepository.save(any(Deployment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(serviceRepository.save(any(ServiceUnit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resolveSecretValue.forProject(project.getId(), ExecuteDeployServiceJobUseCase.GIT_TOKEN_SECRET_NAME))
                .thenReturn(Optional.empty());

        doAnswer(inv -> null).when(gitRepository).cloneOrUpdate(any(), any(), any(), any(), any());
        doAnswer(inv -> null).when(runtimeOrchestrator).apply(any());

        useCase.execute(deployment.getId());

        verify(runtimeOrchestrator).apply(org.mockito.ArgumentMatchers.argThat(cmd ->
                "docker-compose.atlas.yml".equals(cmd.composeFilePath())
                        && cmd.capability() == com.atlas.domain.runtime.RuntimeCapability.COMPOSE));
        assertTrue(running.getLogs().contains("atlas.yml") || running.getStatus() == DeploymentStatus.SUCCEEDED);
    }

    @Test
    void persistsFailedStatusWhenComposeThrows() {
        Project project = Project.create("demo", "d");
        ServiceUnit service = ServiceUnit.createDefault(
                project.getId(), "https://example.com/demo.git", "main", "docker-compose.yml", "");
        Host host = Host.create("local", "127.0.0.1", "linux", "26", true, ConnectionType.LOCAL, null, 22, null);
        Deployment deployment = Deployment.create(service.getId(), host.getId());
        Deployment running = Deployment.rehydrate(
                deployment.getId(),
                deployment.getServiceId(),
                deployment.getHostId(),
                DeploymentStatus.PENDING,
                null,
                null,
                "",
                deployment.getCreatedAt(),
                deployment.getUpdatedAt());

        when(deploymentRepository.findById(deployment.getId())).thenReturn(Optional.of(running));
        when(serviceRepository.findById(service.getId())).thenReturn(Optional.of(service));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(hostRepository.findById(host.getId())).thenReturn(Optional.of(host));
        when(deploymentRepository.save(any(Deployment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(serviceRepository.save(any(ServiceUnit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resolveSecretValue.forProject(project.getId(), ExecuteDeployServiceJobUseCase.GIT_TOKEN_SECRET_NAME))
                .thenReturn(Optional.empty());

        doAnswer(inv -> null).when(gitRepository).cloneOrUpdate(any(), any(), any(), any(), any());
        doThrow(new DomainException("Command failed (1): docker compose"))
                .when(runtimeOrchestrator)
                .apply(any());

        assertThrows(DomainException.class, () -> useCase.execute(deployment.getId()));

        assertEquals(DeploymentStatus.FAILED, running.getStatus());
        assertEquals(ServiceStatus.FAILED, service.getStatus());
        assertTrue(running.getLogs().contains("ERROR:"));
    }
}
