package com.atlas.application.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnableAutoDeployUseCaseTest {

    @Mock
    private PipelineRepositoryPort pipelineRepository;

    @Mock
    private ServiceRepositoryPort serviceRepository;

    @Mock
    private HostRepositoryPort hostRepository;

    @Mock
    private ProjectAuthorizationService authorizationService;

    @Mock
    private ResolveSecretValueUseCase resolveSecretValue;

    @Mock
    private GitProviderWebhookPort gitProviderWebhook;

    @InjectMocks
    private EnableAutoDeployUseCase useCase;

    @Test
    void createsPipelineWithoutHostPinAndRegistersGithubWebhook() {
        UUID projectId = UUID.randomUUID();
        // Reelpath-safe: repo + branch + atlas.yml compose (no fixed host / composePath pin)
        ServiceUnit service = ServiceUnit.createDefault(
                projectId,
                "https://github.com/AdriPardo/reelpath.git",
                "main",
                null,
                "reelpath.atlasops.dev");

        when(serviceRepository.findById(service.getId())).thenReturn(Optional.of(service));
        when(pipelineRepository.findByServiceId(service.getId())).thenReturn(List.of());
        when(pipelineRepository.existsByProjectIdAndName(projectId, "auto-deploy")).thenReturn(false);
        when(pipelineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(resolveSecretValue.forProject(projectId, ExecuteDeployServiceJobUseCase.GIT_TOKEN_SECRET_NAME))
                .thenReturn(Optional.of("ghp_test"));
        when(gitProviderWebhook.registerPushWebhook(any(), any(), any(), any()))
                .thenReturn(GitProviderWebhookPort.RegisterResult.ok("created", "99"));

        EnableAutoDeployUseCase.Result result = useCase.execute(
                new EnableAutoDeployUseCase.EnableAutoDeployCommand(
                        service.getId(), null, "https://atlas.atlasops.dev"));

        assertTrue(result.created());
        assertNull(result.pipeline().getHostId());
        assertTrue(result.githubWebhookRegistered());
        assertEquals("main", result.trackedBranch());
        assertTrue(result.webhookUrl().startsWith("https://atlas.atlasops.dev/api/v1/webhooks/git/"));
        verify(authorizationService).require(projectId, ProjectPermission.WRITE);
        verify(hostRepository, never()).findById(any());

        ArgumentCaptor<Pipeline> pipelineCap = ArgumentCaptor.forClass(Pipeline.class);
        verify(pipelineRepository).save(pipelineCap.capture());
        assertNull(pipelineCap.getValue().getHostId());

        ArgumentCaptor<String> urlCap = ArgumentCaptor.forClass(String.class);
        verify(gitProviderWebhook)
                .registerPushWebhook(eq(service.getRepositoryUrl()), urlCap.capture(), any(), eq("ghp_test"));
        assertEquals(result.webhookUrl(), urlCap.getValue());
    }

    @Test
    void pinsHostWhenExplicitHostIdProvided() {
        UUID projectId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        ServiceUnit service = ServiceUnit.createDefault(
                projectId,
                "https://github.com/AdriPardo/reelpath.git",
                "main",
                null,
                "reelpath.atlasops.dev");

        when(serviceRepository.findById(service.getId())).thenReturn(Optional.of(service));
        when(pipelineRepository.findByServiceId(service.getId())).thenReturn(List.of());
        when(hostRepository.findById(hostId)).thenReturn(Optional.of(com.atlas.domain.host.Host.create(
                "atlas-local",
                "127.0.0.1",
                "linux",
                "",
                true,
                com.atlas.domain.host.ConnectionType.LOCAL,
                null,
                22,
                null)));
        when(pipelineRepository.existsByProjectIdAndName(projectId, "auto-deploy")).thenReturn(false);
        when(pipelineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(resolveSecretValue.forProject(projectId, ExecuteDeployServiceJobUseCase.GIT_TOKEN_SECRET_NAME))
                .thenReturn(Optional.empty());

        EnableAutoDeployUseCase.Result result = useCase.execute(
                new EnableAutoDeployUseCase.EnableAutoDeployCommand(
                        service.getId(), hostId, "https://atlas.atlasops.dev"));

        assertTrue(result.created());
        assertEquals(hostId, result.pipeline().getHostId());
    }

    @Test
    void reusesExistingPipelineWithoutCreating() {
        UUID projectId = UUID.randomUUID();
        ServiceUnit service = ServiceUnit.createDefault(
                projectId,
                "https://github.com/AdriPardo/reelpath.git",
                "main",
                null,
                null);
        Pipeline existing = Pipeline.create(projectId, "auto-deploy", service.getId(), null);

        when(serviceRepository.findById(service.getId())).thenReturn(Optional.of(service));
        when(pipelineRepository.findByServiceId(service.getId())).thenReturn(List.of(existing));
        when(resolveSecretValue.forProject(projectId, ExecuteDeployServiceJobUseCase.GIT_TOKEN_SECRET_NAME))
                .thenReturn(Optional.empty());

        EnableAutoDeployUseCase.Result result = useCase.execute(
                new EnableAutoDeployUseCase.EnableAutoDeployCommand(
                        service.getId(), null, "https://atlas.atlasops.dev"));

        assertFalse(result.created());
        assertFalse(result.githubWebhookRegistered());
        assertEquals(existing.getId(), result.pipeline().getId());
        assertNull(result.pipeline().getHostId());
        verify(pipelineRepository, never()).save(any());
        verify(gitProviderWebhook, never()).registerPushWebhook(any(), any(), any(), any());
        assertTrue(result.setupInstructions().contains("Payload URL"));
    }
}
