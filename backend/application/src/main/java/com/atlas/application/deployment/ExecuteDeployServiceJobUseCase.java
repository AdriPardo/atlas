package com.atlas.application.deployment;

import com.atlas.application.observability.EvaluateProductAlertsUseCase;
import com.atlas.application.port.out.ContainerRuntimePort;
import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.GitRepositoryPort;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.application.secret.ResolveSecretValueUseCase;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.host.ConnectionType;
import com.atlas.domain.host.Host;
import com.atlas.domain.observability.AlertEventType;
import com.atlas.domain.project.Project;
import com.atlas.domain.project.ProjectStatus;
import com.atlas.domain.service.ServiceStatus;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.domain.shared.DomainException;
import com.atlas.domain.shared.NotFoundException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ExecuteDeployServiceJobUseCase {

    public static final String GIT_TOKEN_SECRET_NAME = "git.token";

    private final DeploymentRepositoryPort deploymentRepository;
    private final ServiceRepositoryPort serviceRepository;
    private final ProjectRepositoryPort projectRepository;
    private final HostRepositoryPort hostRepository;
    private final GitRepositoryPort gitRepository;
    private final ContainerRuntimePort containerRuntime;
    private final ResolveSecretValueUseCase resolveSecretValue;
    private final WorkspacePathResolver workspacePathResolver;
    private final EvaluateProductAlertsUseCase evaluateProductAlertsUseCase;
    private final TransactionTemplate transactionTemplate;

    public ExecuteDeployServiceJobUseCase(
            DeploymentRepositoryPort deploymentRepository,
            ServiceRepositoryPort serviceRepository,
            ProjectRepositoryPort projectRepository,
            HostRepositoryPort hostRepository,
            GitRepositoryPort gitRepository,
            ContainerRuntimePort containerRuntime,
            ResolveSecretValueUseCase resolveSecretValue,
            WorkspacePathResolver workspacePathResolver,
            EvaluateProductAlertsUseCase evaluateProductAlertsUseCase,
            PlatformTransactionManager transactionManager) {
        this.deploymentRepository = deploymentRepository;
        this.serviceRepository = serviceRepository;
        this.projectRepository = projectRepository;
        this.hostRepository = hostRepository;
        this.gitRepository = gitRepository;
        this.containerRuntime = containerRuntime;
        this.resolveSecretValue = resolveSecretValue;
        this.workspacePathResolver = workspacePathResolver;
        this.evaluateProductAlertsUseCase = evaluateProductAlertsUseCase;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Runs outside a single long transaction so compose/git logs commit incrementally and failure
     * status is not rolled back when the worker rethrows.
     */
    public void execute(UUID deploymentId) {
        Loaded loaded = transactionTemplate.execute(status -> load(deploymentId));
        if (loaded == null) {
            throw new NotFoundException("Deployment not found: " + deploymentId);
        }

        transactionTemplate.executeWithoutResult(status -> {
            Deployment running = deploymentRepository
                    .findById(deploymentId)
                    .orElseThrow(() -> new NotFoundException("Deployment not found: " + deploymentId));
            running.markRunning();
            deploymentRepository.save(running);
        });

        Path workspace = workspacePathResolver.resolve(deploymentId);
        Consumer<String> logSink = line -> transactionTemplate.executeWithoutResult(status -> {
            Deployment current = deploymentRepository
                    .findById(deploymentId)
                    .orElseThrow(() -> new NotFoundException("Deployment not found: " + deploymentId));
            current.appendLog(line);
            deploymentRepository.save(current);
        });

        try {
            logSink.accept(
                    "Starting deploy for service " + loaded.service().getName() + " (project "
                            + loaded.project().getName() + ")");
            Optional<String> gitToken = resolveSecretValue.byName(GIT_TOKEN_SECRET_NAME);
            gitRepository.cloneOrUpdate(
                    loaded.service().getRepositoryUrl(),
                    loaded.service().getBranch(),
                    workspace,
                    gitToken,
                    logSink);

            Optional<String> sshKey = resolveSshKey(loaded.host());
            containerRuntime.composeUp(
                    loaded.host(), workspace, loaded.service().getComposePath(), sshKey, logSink);

            transactionTemplate.executeWithoutResult(status -> {
                Deployment succeeded = deploymentRepository
                        .findById(deploymentId)
                        .orElseThrow(() -> new NotFoundException("Deployment not found: " + deploymentId));
                succeeded.appendLog("Deploy finished successfully");
                succeeded.markSucceeded();
                deploymentRepository.save(succeeded);

                ServiceUnit service = serviceRepository
                        .findById(loaded.service().getId())
                        .orElseThrow(() -> new NotFoundException("Service not found: " + loaded.service().getId()));
                Project project = projectRepository
                        .findById(loaded.project().getId())
                        .orElseThrow(() -> new NotFoundException("Project not found: " + loaded.project().getId()));
                updateStatuses(service, project, ServiceStatus.RUNNING, ProjectStatus.RUNNING);
            });
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            transactionTemplate.executeWithoutResult(status -> {
                Deployment failed = deploymentRepository
                        .findById(deploymentId)
                        .orElseThrow(() -> new NotFoundException("Deployment not found: " + deploymentId));
                failed.markFailed("ERROR: " + message);
                deploymentRepository.save(failed);

                ServiceUnit service = serviceRepository
                        .findById(loaded.service().getId())
                        .orElseThrow(() -> new NotFoundException("Service not found: " + loaded.service().getId()));
                Project project = projectRepository
                        .findById(loaded.project().getId())
                        .orElseThrow(() -> new NotFoundException("Project not found: " + loaded.project().getId()));
                updateStatuses(service, project, ServiceStatus.FAILED, ProjectStatus.FAILED);
                evaluateProductAlertsUseCase.execute(
                        AlertEventType.DEPLOY_FAILED,
                        project.getId(),
                        "Deploy failed: " + message,
                        "deployment",
                        failed.getId());
            });
            throw new DomainException("Deploy failed: " + message);
        }
    }

    private Loaded load(UUID deploymentId) {
        Deployment deployment = deploymentRepository
                .findById(deploymentId)
                .orElseThrow(() -> new NotFoundException("Deployment not found: " + deploymentId));
        ServiceUnit service = serviceRepository
                .findById(deployment.getServiceId())
                .orElseThrow(() -> new NotFoundException("Service not found: " + deployment.getServiceId()));
        Project project = projectRepository
                .findById(service.getProjectId())
                .orElseThrow(() -> new NotFoundException("Project not found: " + service.getProjectId()));
        Host host = hostRepository
                .findById(deployment.getHostId())
                .orElseThrow(() -> new NotFoundException("Host not found: " + deployment.getHostId()));
        return new Loaded(service, project, host);
    }

    private void updateStatuses(
            ServiceUnit service, Project project, ServiceStatus serviceStatus, ProjectStatus projectStatus) {
        service.updateStatus(serviceStatus);
        serviceRepository.save(service);
        project.updateStatus(projectStatus);
        projectRepository.save(project);
    }

    private Optional<String> resolveSshKey(Host host) {
        if (host.getConnectionType() != ConnectionType.SSH) {
            return Optional.empty();
        }
        if (host.getSshPrivateKeySecretId() == null) {
            throw new DomainException(
                    "SSH host requires sshPrivateKeySecretId (create a secret and link it to the host)");
        }
        return Optional.of(resolveSecretValue.byId(host.getSshPrivateKeySecretId()));
    }

    private record Loaded(ServiceUnit service, Project project, Host host) {}

    public interface WorkspacePathResolver {
        Path resolve(UUID deploymentId);
    }
}
