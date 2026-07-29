package com.atlas.application.deployment;

import com.atlas.application.manifest.ComposePathResolver;
import com.atlas.application.networking.EnsureDomainDnsCnameUseCase;
import com.atlas.application.networking.EnsureDomainTunnelIngressUseCase;
import com.atlas.application.observability.EvaluateProductAlertsUseCase;
import com.atlas.application.port.out.CloudflareTunnelPort;
import com.atlas.application.port.out.ContainerRuntimePort;
import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.DnsProviderPort;
import com.atlas.application.port.out.DomainRepositoryPort;
import com.atlas.application.port.out.GitRepositoryPort;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.application.secret.ResolveSecretValueUseCase;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.host.ConnectionType;
import com.atlas.domain.host.Host;
import com.atlas.domain.networking.Domain;
import com.atlas.domain.observability.AlertEventType;
import com.atlas.domain.project.Project;
import com.atlas.domain.project.ProjectStatus;
import com.atlas.domain.service.ServiceExposure;
import com.atlas.domain.service.ServiceStatus;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.domain.shared.DomainException;
import com.atlas.domain.shared.NotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
    private final DomainRepositoryPort domainRepository;
    private final GitRepositoryPort gitRepository;
    private final ContainerRuntimePort containerRuntime;
    private final ResolveSecretValueUseCase resolveSecretValue;
    private final WorkspacePathResolver workspacePathResolver;
    private final EvaluateProductAlertsUseCase evaluateProductAlertsUseCase;
    private final EnsureDomainTunnelIngressUseCase ensureDomainTunnelIngressUseCase;
    private final EnsureDomainDnsCnameUseCase ensureDomainDnsCnameUseCase;
    private final ComposePathResolver composePathResolver;
    private final TransactionTemplate transactionTemplate;

    public ExecuteDeployServiceJobUseCase(
            DeploymentRepositoryPort deploymentRepository,
            ServiceRepositoryPort serviceRepository,
            ProjectRepositoryPort projectRepository,
            HostRepositoryPort hostRepository,
            DomainRepositoryPort domainRepository,
            GitRepositoryPort gitRepository,
            ContainerRuntimePort containerRuntime,
            ResolveSecretValueUseCase resolveSecretValue,
            WorkspacePathResolver workspacePathResolver,
            EvaluateProductAlertsUseCase evaluateProductAlertsUseCase,
            EnsureDomainTunnelIngressUseCase ensureDomainTunnelIngressUseCase,
            EnsureDomainDnsCnameUseCase ensureDomainDnsCnameUseCase,
            PlatformTransactionManager transactionManager) {
        this(
                deploymentRepository,
                serviceRepository,
                projectRepository,
                hostRepository,
                domainRepository,
                gitRepository,
                containerRuntime,
                resolveSecretValue,
                workspacePathResolver,
                evaluateProductAlertsUseCase,
                ensureDomainTunnelIngressUseCase,
                ensureDomainDnsCnameUseCase,
                new ComposePathResolver(),
                transactionManager);
    }

    ExecuteDeployServiceJobUseCase(
            DeploymentRepositoryPort deploymentRepository,
            ServiceRepositoryPort serviceRepository,
            ProjectRepositoryPort projectRepository,
            HostRepositoryPort hostRepository,
            DomainRepositoryPort domainRepository,
            GitRepositoryPort gitRepository,
            ContainerRuntimePort containerRuntime,
            ResolveSecretValueUseCase resolveSecretValue,
            WorkspacePathResolver workspacePathResolver,
            EvaluateProductAlertsUseCase evaluateProductAlertsUseCase,
            EnsureDomainTunnelIngressUseCase ensureDomainTunnelIngressUseCase,
            EnsureDomainDnsCnameUseCase ensureDomainDnsCnameUseCase,
            ComposePathResolver composePathResolver,
            PlatformTransactionManager transactionManager) {
        this.deploymentRepository = deploymentRepository;
        this.serviceRepository = serviceRepository;
        this.projectRepository = projectRepository;
        this.hostRepository = hostRepository;
        this.domainRepository = domainRepository;
        this.gitRepository = gitRepository;
        this.containerRuntime = containerRuntime;
        this.resolveSecretValue = resolveSecretValue;
        this.workspacePathResolver = workspacePathResolver;
        this.evaluateProductAlertsUseCase = evaluateProductAlertsUseCase;
        this.ensureDomainTunnelIngressUseCase = ensureDomainTunnelIngressUseCase;
        this.ensureDomainDnsCnameUseCase = ensureDomainDnsCnameUseCase;
        this.composePathResolver = composePathResolver;
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
            Optional<String> gitToken =
                    resolveSecretValue.forProject(loaded.project().getId(), GIT_TOKEN_SECRET_NAME);
            gitRepository.cloneOrUpdate(
                    loaded.service().getRepositoryUrl(),
                    loaded.service().getBranch(),
                    workspace,
                    gitToken,
                    logSink);

            seedAtlasEnvFile(workspace, loaded.service(), logSink);

            ComposePathResolver.Resolution compose =
                    composePathResolver.resolve(workspace, loaded.service().getComposePath());
            logSink.accept("Using " + compose.describe());

            Optional<String> sshKey = resolveSshKey(loaded.host());
            containerRuntime.composeUp(
                    loaded.host(), workspace, compose.composeFilePath(), sshKey, logSink);

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

            ensurePublicTunnelIngress(loaded.service(), logSink);
            ensurePublicDnsCname(loaded.service(), logSink);
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

    /**
     * Customer apps often ship {@code .env.atlas.example}. Materialize {@code .env} so Compose
     * interpolates DOMAIN / secrets without operator SSH. Existing {@code .env} is left untouched.
     */
    private void seedAtlasEnvFile(Path workspace, ServiceUnit service, Consumer<String> logSink) {
        Path envFile = workspace.resolve(".env");
        if (Files.exists(envFile)) {
            return;
        }
        Path example = workspace.resolve(".env.atlas.example");
        try {
            String body;
            if (Files.exists(example)) {
                body = Files.readString(example);
                logSink.accept("Seeding .env from .env.atlas.example");
            } else {
                body = "";
                logSink.accept("Seeding minimal .env (no .env.atlas.example in repo)");
            }
            String domain = service.getDomain() == null ? "" : service.getDomain().trim();
            if (!domain.isEmpty()) {
                if (body.lines().anyMatch(line -> line.startsWith("DOMAIN="))) {
                    body = body.lines()
                            .map(line -> line.startsWith("DOMAIN=") ? "DOMAIN=" + domain : line)
                            .collect(java.util.stream.Collectors.joining("\n", "", "\n"));
                } else {
                    body = "DOMAIN=" + domain + "\n" + body;
                }
            }
            if (!body.isEmpty() && !body.endsWith("\n")) {
                body = body + "\n";
            }
            Files.writeString(envFile, body, StandardOpenOption.CREATE_NEW);
        } catch (Exception ex) {
            throw new DomainException("Failed to seed .env for deploy: " + ex.getMessage());
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

    /**
     * Autopilot PUBLIC: after compose is up, try Cloudflare Tunnel hostname registration (or log
     * copy-ready ingress when API credentials are absent). Never fails the deploy.
     */
    private void ensurePublicTunnelIngress(ServiceUnit service, Consumer<String> logSink) {
        if (service.getExposure() != ServiceExposure.PUBLIC) {
            return;
        }
        String hostname = service.getDomain();
        if (hostname == null || hostname.isBlank()) {
            return;
        }
        try {
            Optional<Domain> domain = domainRepository.findByProjectId(service.getProjectId()).stream()
                    .filter(d -> hostname.equalsIgnoreCase(d.getHostname()))
                    .findFirst();
            if (domain.isEmpty()) {
                logSink.accept("Tunnel: no Domain stub for " + hostname + " — skip");
                return;
            }
            CloudflareTunnelPort.EnsureResult result =
                    ensureDomainTunnelIngressUseCase.executeAsSystem(domain.get());
            logSink.accept("Tunnel [" + result.mode() + "]: " + result.message());
            if (result.mode() == CloudflareTunnelPort.EnsureMode.MANUAL
                    || result.mode() == CloudflareTunnelPort.EnsureMode.FAILED) {
                logSink.accept("Tunnel ingress (copy into Zero Trust):\n" + result.ingress().copyBlock());
            }
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            logSink.accept("Tunnel assist skipped: " + message);
        }
    }

    /**
     * Autopilot PUBLIC: upsert Cloudflare DNS CNAME → tunnel target (or log copy-ready record).
     * Never fails the deploy.
     */
    private void ensurePublicDnsCname(ServiceUnit service, Consumer<String> logSink) {
        if (service.getExposure() != ServiceExposure.PUBLIC) {
            return;
        }
        String hostname = service.getDomain();
        if (hostname == null || hostname.isBlank()) {
            return;
        }
        try {
            Optional<Domain> domain = domainRepository.findByProjectId(service.getProjectId()).stream()
                    .filter(d -> hostname.equalsIgnoreCase(d.getHostname()))
                    .findFirst();
            if (domain.isEmpty()) {
                logSink.accept("DNS CNAME: no Domain stub for " + hostname + " — skip");
                return;
            }
            DnsProviderPort.CnameEnsureResult result =
                    ensureDomainDnsCnameUseCase.executeAsSystem(domain.get());
            logSink.accept("DNS CNAME [" + result.mode() + "]: " + result.message());
            if (result.mode() == DnsProviderPort.CnameEnsureMode.MANUAL
                    || result.mode() == DnsProviderPort.CnameEnsureMode.FAILED) {
                logSink.accept("DNS CNAME (copy into Cloudflare DNS):\n" + result.spec().copyBlock());
            }
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            logSink.accept("DNS CNAME assist skipped: " + message);
        }
    }

    private record Loaded(ServiceUnit service, Project project, Host host) {}

    public interface WorkspacePathResolver {
        Path resolve(UUID deploymentId);
    }
}
