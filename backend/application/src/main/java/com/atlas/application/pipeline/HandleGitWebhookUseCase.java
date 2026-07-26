package com.atlas.application.pipeline;

import com.atlas.application.port.out.PipelineRepositoryPort;
import com.atlas.application.port.out.WebhookRateLimiterPort;
import com.atlas.domain.pipeline.Pipeline;
import com.atlas.domain.pipeline.PipelineRun;
import com.atlas.domain.shared.ForbiddenException;
import com.atlas.domain.shared.NotFoundException;
import com.atlas.domain.shared.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HandleGitWebhookUseCase {

    private final PipelineRepositoryPort pipelineRepository;
    private final RunPipelineUseCase runPipelineUseCase;
    private final GitWebhookSignatureVerifier signatureVerifier;
    private final WebhookRateLimiterPort rateLimiter;

    @Transactional
    public PipelineRun execute(String token, byte[] body, String githubSignature, String giteaSignature) {
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
        return runPipelineUseCase.executeTrusted(pipeline.getId(), "webhook");
    }
}
