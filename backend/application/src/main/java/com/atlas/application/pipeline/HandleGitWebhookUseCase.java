package com.atlas.application.pipeline;

import com.atlas.application.port.out.PipelineRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.application.port.out.WebhookRateLimiterPort;
import com.atlas.domain.pipeline.Pipeline;
import com.atlas.domain.pipeline.PipelineRun;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.domain.shared.ForbiddenException;
import com.atlas.domain.shared.NotFoundException;
import com.atlas.domain.shared.TooManyRequestsException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HandleGitWebhookUseCase {

    private final PipelineRepositoryPort pipelineRepository;
    private final ServiceRepositoryPort serviceRepository;
    private final RunPipelineUseCase runPipelineUseCase;
    private final GitWebhookSignatureVerifier signatureVerifier;
    private final WebhookRateLimiterPort rateLimiter;

    /**
     * @return empty when the event was intentionally ignored (wrong branch, ping, non-push)
     */
    @Transactional
    public Optional<PipelineRun> execute(
            String token,
            byte[] body,
            String githubSignature,
            String giteaSignature,
            String githubEvent,
            String giteaEvent) {
        if (token == null || token.isBlank()) {
            throw new NotFoundException("Webhook token not found");
        }
        String trimmed = token.trim();
        if (!rateLimiter.tryAcquire(trimmed)) {
            throw new TooManyRequestsException("Webhook rate limit exceeded; retry later");
        }
        Pipeline pipeline = pipelineRepository
                .findByWebhookToken(trimmed)
                .orElseThrow(() -> new NotFoundException("Webhook token not found"));
        if (!signatureVerifier.isValid(body, pipeline.getWebhookToken(), githubSignature, giteaSignature)) {
            throw new ForbiddenException("Invalid webhook signature");
        }

        ServiceUnit service = serviceRepository
                .findById(pipeline.getServiceId())
                .orElseThrow(() -> new NotFoundException("Service not found: " + pipeline.getServiceId()));

        Optional<String> ignore =
                GitWebhookEventEvaluator.ignoreReason(body, githubEvent, giteaEvent, service.getBranch());
        if (ignore.isPresent()) {
            return Optional.empty();
        }

        return Optional.of(runPipelineUseCase.executeTrusted(pipeline.getId(), "webhook"));
    }
}
