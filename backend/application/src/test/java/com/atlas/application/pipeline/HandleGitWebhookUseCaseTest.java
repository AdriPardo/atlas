package com.atlas.application.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.port.out.PipelineRepositoryPort;
import com.atlas.application.port.out.WebhookRateLimiterPort;
import com.atlas.domain.pipeline.Pipeline;
import com.atlas.domain.pipeline.PipelineRun;
import com.atlas.domain.shared.ForbiddenException;
import com.atlas.domain.shared.NotFoundException;
import com.atlas.domain.shared.TooManyRequestsException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HandleGitWebhookUseCaseTest {

    @Mock
    private PipelineRepositoryPort pipelineRepository;

    @Mock
    private RunPipelineUseCase runPipelineUseCase;

    @Mock
    private GitWebhookSignatureVerifier signatureVerifier;

    @Mock
    private WebhookRateLimiterPort rateLimiter;

    @InjectMocks
    private HandleGitWebhookUseCase useCase;

    @Test
    void runsPipelineWhenTokenMatches() {
        Pipeline pipeline = Pipeline.create(UUID.randomUUID(), "hook", UUID.randomUUID(), UUID.randomUUID());
        when(rateLimiter.tryAcquire(pipeline.getWebhookToken())).thenReturn(true);
        when(pipelineRepository.findByWebhookToken(pipeline.getWebhookToken())).thenReturn(Optional.of(pipeline));
        when(signatureVerifier.isValid(any(), eq(pipeline.getWebhookToken()), eq(null), eq(null))).thenReturn(true);
        PipelineRun run = PipelineRun.start(pipeline.getId(), "webhook");
        when(runPipelineUseCase.executeTrusted(pipeline.getId(), "webhook")).thenReturn(run);

        PipelineRun result = useCase.execute(pipeline.getWebhookToken(), "{}".getBytes(), null, null);

        assertEquals("webhook", result.getTriggeredBy());
        verify(runPipelineUseCase).executeTrusted(pipeline.getId(), "webhook");
    }

    @Test
    void rejectsUnknownToken() {
        when(rateLimiter.tryAcquire("atk_missing")).thenReturn(true);
        when(pipelineRepository.findByWebhookToken("atk_missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute("atk_missing", null, null, null));
        verify(runPipelineUseCase, never()).executeTrusted(any(), anyString());
    }

    @Test
    void rejectsInvalidSignature() {
        Pipeline pipeline = Pipeline.create(UUID.randomUUID(), "hook", UUID.randomUUID(), UUID.randomUUID());
        when(rateLimiter.tryAcquire(pipeline.getWebhookToken())).thenReturn(true);
        when(pipelineRepository.findByWebhookToken(pipeline.getWebhookToken())).thenReturn(Optional.of(pipeline));
        when(signatureVerifier.isValid(any(), eq(pipeline.getWebhookToken()), eq("sha256=bad"), eq(null)))
                .thenReturn(false);

        assertThrows(
                ForbiddenException.class,
                () -> useCase.execute(pipeline.getWebhookToken(), "{}".getBytes(), "sha256=bad", null));
    }

    @Test
    void rateLimitsPerToken() {
        when(rateLimiter.tryAcquire("atk_busy")).thenReturn(false);

        assertThrows(TooManyRequestsException.class, () -> useCase.execute("atk_busy", null, null, null));
        verify(pipelineRepository, never()).findByWebhookToken(anyString());
    }
}
