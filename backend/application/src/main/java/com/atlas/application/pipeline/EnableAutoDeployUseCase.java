package com.atlas.application.pipeline;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.deployment.ExecuteDeployServiceJobUseCase;
import com.atlas.application.port.out.GitProviderWebhookPort;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.application.port.out.PipelineRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.application.secret.ResolveSecretValueUseCase;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.pipeline.Pipeline;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.domain.shared.DomainException;
import com.atlas.domain.shared.NotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-click auto-deploy: ensure a default Pipeline for a service, optionally register a GitHub
 * push webhook when {@code git.token} + absolute public base URL are available.
 *
 * <p>Default pipeline omits {@code hostId} so each webhook/run uses Autopilot placement (SHARED
 * unless the caller later pins a host). Explicit {@code hostId} remains an advanced override.
 */
@Service
@RequiredArgsConstructor
public class EnableAutoDeployUseCase {

    public static final String DEFAULT_PIPELINE_NAME = "auto-deploy";

    private final PipelineRepositoryPort pipelineRepository;
    private final ServiceRepositoryPort serviceRepository;
    private final HostRepositoryPort hostRepository;
    private final ProjectAuthorizationService authorizationService;
    private final ResolveSecretValueUseCase resolveSecretValue;
    private final GitProviderWebhookPort gitProviderWebhook;

    @Transactional
    public Result execute(EnableAutoDeployCommand command) {
        ServiceUnit service = serviceRepository
                .findById(command.serviceId())
                .orElseThrow(() -> new NotFoundException("Service not found: " + command.serviceId()));
        authorizationService.require(service.getProjectId(), ProjectPermission.WRITE);

        if (service.getRepositoryUrl() == null || service.getRepositoryUrl().isBlank()) {
            throw new DomainException("Service has no repository URL; cannot enable auto-deploy");
        }

        List<Pipeline> existing = pipelineRepository.findByServiceId(service.getId());
        boolean created;
        Pipeline pipeline;
        if (!existing.isEmpty()) {
            pipeline = existing.get(0);
            created = false;
        } else {
            UUID pinnedHostId = resolveOptionalPin(command.hostId());
            String name = uniquePipelineName(service.getProjectId(), DEFAULT_PIPELINE_NAME);
            pipeline = pipelineRepository.save(
                    Pipeline.create(service.getProjectId(), name, service.getId(), pinnedHostId));
            created = true;
        }

        String publicBase = trimTrailingSlash(command.publicBaseUrl());
        String webhookPath = "/api/v1/webhooks/git/" + pipeline.getWebhookToken();
        String webhookUrl = publicBase == null ? webhookPath : publicBase + webhookPath;

        GitProviderWebhookPort.RegisterResult registration = registerIfPossible(service, webhookUrl, pipeline);

        return new Result(
                pipeline,
                created,
                webhookUrl,
                service.getBranch(),
                registration.registered(),
                registration.message(),
                registration.providerHookId().orElse(null),
                setupInstructions(service, webhookUrl, pipeline.getWebhookToken(), registration));
    }

    private UUID resolveOptionalPin(UUID hostId) {
        if (hostId == null) {
            return null;
        }
        if (hostRepository.findById(hostId).isEmpty()) {
            throw new NotFoundException("Host not found: " + hostId);
        }
        return hostId;
    }

    private GitProviderWebhookPort.RegisterResult registerIfPossible(
            ServiceUnit service, String webhookUrl, Pipeline pipeline) {
        if (!GithubRepositoryUrlParser.isGithub(service.getRepositoryUrl())) {
            return GitProviderWebhookPort.RegisterResult.skipped(
                    "Repository is not GitHub; register the webhook manually.");
        }
        if (!webhookUrl.startsWith("http://") && !webhookUrl.startsWith("https://")) {
            return GitProviderWebhookPort.RegisterResult.skipped(
                    "No absolute public base URL; copy the webhook URL into GitHub manually.");
        }
        Optional<String> token =
                resolveSecretValue.forProject(service.getProjectId(), ExecuteDeployServiceJobUseCase.GIT_TOKEN_SECRET_NAME);
        if (token.isEmpty() || token.get().isBlank()) {
            return GitProviderWebhookPort.RegisterResult.skipped(
                    "No git.token secret on this project; copy the webhook URL into GitHub manually.");
        }
        return gitProviderWebhook.registerPushWebhook(
                service.getRepositoryUrl(), webhookUrl, pipeline.getWebhookToken(), token.get());
    }

    private String uniquePipelineName(UUID projectId, String base) {
        if (!pipelineRepository.existsByProjectIdAndName(projectId, base)) {
            return base;
        }
        for (int i = 2; i < 50; i++) {
            String candidate = base + "-" + i;
            if (!pipelineRepository.existsByProjectIdAndName(projectId, candidate)) {
                return candidate;
            }
        }
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String setupInstructions(
            ServiceUnit service,
            String webhookUrl,
            String secret,
            GitProviderWebhookPort.RegisterResult registration) {
        if (registration.registered()) {
            return "GitHub push webhook registered. Pushes to branch '"
                    + service.getBranch()
                    + "' will deploy automatically.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Add a GitHub webhook (Settings → Webhooks → Add webhook):\n");
        sb.append("1. Payload URL: ").append(webhookUrl).append('\n');
        sb.append("2. Content type: application/json\n");
        sb.append("3. Secret: the pipeline webhook token (same value as the path token)\n");
        sb.append("4. Events: Just the push event\n");
        sb.append("5. Atlas deploys only pushes to branch '")
                .append(service.getBranch())
                .append("'.\n");
        if (registration.message() != null && !registration.message().isBlank()) {
            sb.append('(').append(registration.message()).append(')');
        }
        // Keep secret out of the default instructions body shown in lists; UI shows token separately.
        if (secret != null && secret.toLowerCase(Locale.ROOT).startsWith("atk_")) {
            // hint only
        }
        return sb.toString();
    }

    public record EnableAutoDeployCommand(UUID serviceId, UUID hostId, String publicBaseUrl) {}

    public record Result(
            Pipeline pipeline,
            boolean created,
            String webhookUrl,
            String trackedBranch,
            boolean githubWebhookRegistered,
            String githubWebhookMessage,
            String githubHookId,
            String setupInstructions) {}
}
