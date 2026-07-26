package com.atlas.application.deployment;

import com.atlas.application.port.out.ApplicationRepositoryPort;
import com.atlas.application.port.out.ContainerRuntimePort;
import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.GitRepositoryPort;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.application.secret.ResolveSecretValueUseCase;
import com.atlas.domain.application.Application;
import com.atlas.domain.application.ApplicationStatus;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.host.ConnectionType;
import com.atlas.domain.host.Host;
import com.atlas.domain.shared.DomainException;
import com.atlas.domain.shared.NotFoundException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExecuteDeployServiceJobUseCase {

    public static final String GIT_TOKEN_SECRET_NAME = "git.token";

    private final DeploymentRepositoryPort deploymentRepository;
    private final ApplicationRepositoryPort applicationRepository;
    private final HostRepositoryPort hostRepository;
    private final GitRepositoryPort gitRepository;
    private final ContainerRuntimePort containerRuntime;
    private final ResolveSecretValueUseCase resolveSecretValue;
    private final WorkspacePathResolver workspacePathResolver;

    @Transactional
    public void execute(UUID deploymentId) {
        Deployment deployment = deploymentRepository
                .findById(deploymentId)
                .orElseThrow(() -> new NotFoundException("Deployment not found: " + deploymentId));
        Application application = applicationRepository
                .findById(deployment.getApplicationId())
                .orElseThrow(() -> new NotFoundException(
                        "Application not found: " + deployment.getApplicationId()));
        Host host = hostRepository
                .findById(deployment.getHostId())
                .orElseThrow(() -> new NotFoundException("Host not found: " + deployment.getHostId()));

        deployment.markRunning();
        deploymentRepository.save(deployment);

        Path workspace = workspacePathResolver.resolve(deployment.getId());
        Consumer<String> logSink = line -> {
            Deployment current = deploymentRepository
                    .findById(deploymentId)
                    .orElseThrow(() -> new NotFoundException("Deployment not found: " + deploymentId));
            current.appendLog(line);
            deploymentRepository.save(current);
        };

        try {
            logSink.accept("Starting deploy for application " + application.getName());
            Optional<String> gitToken = resolveSecretValue.byName(GIT_TOKEN_SECRET_NAME);
            gitRepository.cloneOrUpdate(
                    application.getRepositoryUrl(),
                    application.getBranch(),
                    workspace,
                    gitToken,
                    logSink);

            Optional<String> sshKey = resolveSshKey(host);
            containerRuntime.composeUp(
                    host, workspace, application.getComposePath(), sshKey, logSink);

            Deployment succeeded = deploymentRepository
                    .findById(deploymentId)
                    .orElseThrow(() -> new NotFoundException("Deployment not found: " + deploymentId));
            succeeded.appendLog("Deploy finished successfully");
            succeeded.markSucceeded();
            deploymentRepository.save(succeeded);

            application.update(
                    application.getName(),
                    application.getDescription(),
                    application.getRepositoryUrl(),
                    application.getBranch(),
                    application.getComposePath(),
                    application.getDomain(),
                    ApplicationStatus.RUNNING);
            applicationRepository.save(application);
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            Deployment failed = deploymentRepository
                    .findById(deploymentId)
                    .orElseThrow(() -> new NotFoundException("Deployment not found: " + deploymentId));
            failed.markFailed("ERROR: " + message);
            deploymentRepository.save(failed);

            application.update(
                    application.getName(),
                    application.getDescription(),
                    application.getRepositoryUrl(),
                    application.getBranch(),
                    application.getComposePath(),
                    application.getDomain(),
                    ApplicationStatus.FAILED);
            applicationRepository.save(application);
            throw new DomainException("Deploy failed: " + message);
        }
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

    public interface WorkspacePathResolver {
        Path resolve(UUID deploymentId);
    }
}
