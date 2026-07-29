package com.atlas.application.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.port.out.PipelineRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.application.port.out.WebhookRateLimiterPort;
import com.atlas.domain.pipeline.Pipeline;
import com.atlas.domain.pipeline.PipelineRun;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.domain.shared.ForbiddenException;
import com.atlas.domain.shared.NotFoundException;
import com.atlas.domain.shared.TooManyRequestsException;
import java.nio.charset.StandardCharsets;
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
    private ServiceRepositoryPort serviceRepository;

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
        ServiceUnit service = ServiceUnit.createDefault(
                pipeline.getProjectId(), "https://github.com/o/r.git", "main", "./docker-compose.yml", null);
        when(rateLimiter.tryAcquire(pipeline.getWebhookToken())).thenReturn(true);
        when(pipelineRepository.findByWebhookToken(pipeline.getWebhookToken())).thenReturn(Optional.of(pipeline));
        when(signatureVerifier.isValid(any(), eq(pipeline.getWebhookToken()), eq(null), eq(null))).thenReturn(true);
        when(serviceRepository.findById(pipeline.getServiceId())).thenReturn(Optional.of(service));
        PipelineRun run = PipelineRun.start(pipeline.getId(), "webhook");
        when(runPipelineUseCase.executeTrusted(pipeline.getId(), "webhook")).thenReturn(run);

        Optional<PipelineRun> result =
                useCase.execute(pipeline.getWebhookToken(), "{}".getBytes(), null, null, null, null);

        assertTrue(result.isPresent());
        assertEquals("webhook", result.get().getTriggeredBy());
        verify(runPipelineUseCase).executeTrusted(pipeline.getId(), "webhook");
    }

    @Test
    void ignoresWrongBranchPush() {
        Pipeline pipeline = Pipeline.create(UUID.randomUUID(), "hook", UUID.randomUUID(), UUID.randomUUID());
        ServiceUnit service = ServiceUnit.createDefault(
                pipeline.getProjectId(), "https://github.com/o/r.git", "main", "./docker-compose.yml", null);
        when(rateLimiter.tryAcquire(pipeline.getWebhookToken())).thenReturn(true);
        when(pipelineRepository.findByWebhookToken(pipeline.getWebhookToken())).thenReturn(Optional.of(pipeline));
        when(signatureVerifier.isValid(any(), eq(pipeline.getWebhookToken()), eq(null), eq(null))).thenReturn(true);
        when(serviceRepository.findById(pipeline.getServiceId())).thenReturn(Optional.of(service));

        byte[] body = "{\"ref\":\"refs/heads/dev\"}".getBytes(StandardCharsets.UTF_8);
        Optional<PipelineRun> result =
                useCase.execute(pipeline.getWebhookToken(), body, null, null, "push", null);

        assertTrue(result.isEmpty());
        verify(runPipelineUseCase, never()).executeTrusted(any(), anyString());
    }

    @Test
    void ignoresPingEvent() {
        Pipeline pipeline = Pipeline.create(UUID.randomUUID(), "hook", UUID.randomUUID(), UUID.randomUUID());
        ServiceUnit service = ServiceUnit.createDefault(
                pipeline.getProjectId(), "https://github.com/o/r.git", "main", "./docker-compose.yml", null);
        when(rateLimiter.tryAcquire(pipeline.getWebhookToken())).thenReturn(true);
        when(pipelineRepository.findByWebhookToken(pipeline.getWebhookToken())).thenReturn(Optional.of(pipeline));
        when(signatureVerifier.isValid(any(), eq(pipeline.getWebhookToken()), eq(null), eq(null))).thenReturn(true);
        when(serviceRepository.findById(pipeline.getServiceId())).thenReturn(Optional.of(service));

        Optional<PipelineRun> result =
                useCase.execute(pipeline.getWebhookToken(), "{}".getBytes(), null, null, "ping", null);

        assertTrue(result.isEmpty());
        verify(runPipelineUseCase, never()).executeTrusted(any(), anyString());
    }

    @Test
    void rejectsUnknownToken() {
        when(rateLimiter.tryAcquire("atk_missing")).thenReturn(true);
        when(pipelineRepository.findByWebhookToken("atk_missing")).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class, () -> useCase.execute("atk_missing", null, null, null, null, null));
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
                () -> useCase.execute(pipeline.getWebhookToken(), "{}".getBytes(), "sha256=bad", null, null, null));
    }

    @Test
    void rateLimitsPerToken() {
        when(rateLimiter.tryAcquire("atk_busy")).thenReturn(false);

        assertThrows(
                TooManyRequestsException.class, () -> useCase.execute("atk_busy", null, null, null, null, null));
        verify(pipelineRepository, never()).findByWebhookToken(anyString());
    }
}
