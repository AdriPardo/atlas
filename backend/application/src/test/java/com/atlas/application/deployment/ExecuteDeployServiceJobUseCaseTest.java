package com.atlas.application.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.observability.EvaluateProductAlertsUseCase;
import com.atlas.application.port.out.ContainerRuntimePort;
import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.GitRepositoryPort;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.application.secret.ResolveSecretValueUseCase;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.deployment.DeploymentStatus;
import com.atlas.domain.host.ConnectionType;
import com.atlas.domain.host.Host;
import com.atlas.domain.project.Project;
import com.atlas.domain.service.ServiceStatus;
import com.atlas.domain.service.ServiceUnit;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExecuteDeployServiceJobUseCaseTest {

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
    private ContainerRuntimePort containerRuntime;

    @Mock
    private ResolveSecretValueUseCase resolveSecretValue;

    @Mock
    private EvaluateProductAlertsUseCase evaluateProductAlertsUseCase;

    private ExecuteDeployServiceJobUseCase useCase;
    private Path workspace;

    @BeforeEach
    void setUp() {
        workspace = Path.of("/tmp/atlas-test-ws");
        useCase = new ExecuteDeployServiceJobUseCase(
                deploymentRepository,
                serviceRepository,
                projectRepository,
                hostRepository,
                gitRepository,
                containerRuntime,
                resolveSecretValue,
                id -> workspace,
                evaluateProductAlertsUseCase);
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
        when(resolveSecretValue.byName(ExecuteDeployServiceJobUseCase.GIT_TOKEN_SECRET_NAME))
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
                    @SuppressWarnings("unchecked")
                    Consumer<String> sink = inv.getArgument(4);
                    sink.accept("compose up ok");
                    return null;
                })
                .when(containerRuntime)
                .composeUp(any(), any(), any(), any(), any());

        useCase.execute(deployment.getId());

        verify(gitRepository)
                .cloneOrUpdate(eq(service.getRepositoryUrl()), eq("main"), eq(workspace), eq(Optional.empty()), any());
        verify(containerRuntime)
                .composeUp(eq(host), eq(workspace), eq("docker-compose.yml"), eq(Optional.empty()), any());
        assertEquals(ServiceStatus.RUNNING, service.getStatus());
        assertTrue(running.getLogs().contains("compose up ok") || running.getStatus() == DeploymentStatus.SUCCEEDED);
    }
}
