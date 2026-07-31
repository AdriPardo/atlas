package com.atlas.application.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.port.out.GitRepositoryPort;
import com.atlas.application.secret.ResolveSecretValueUseCase;
import com.atlas.domain.project.Project;
import com.atlas.domain.runtime.RuntimeCapability;
import com.atlas.domain.service.ServiceUnit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResolvePlacementRuntimeCapabilityUseCaseTest {

    @Mock
    private GitRepositoryPort gitRepository;

    @Mock
    private ResolveSecretValueUseCase resolveSecretValue;

    @Mock
    private ResolvePlacementRuntimeCapabilityUseCase.PlacementWorkspacePathResolver workspacePathResolver;

    @InjectMocks
    private ResolvePlacementRuntimeCapabilityUseCase useCase;

    @TempDir
    Path tempDir;

    @Test
    void returnsPodmanWhenManifestDeclaresPodmanCompose() throws Exception {
        Project project = Project.create("demo", "d");
        ServiceUnit service = ServiceUnit.createDefault(
                project.getId(), "https://git.example/demo.git", "main", "", "");
        when(workspacePathResolver.resolve(service.getId())).thenReturn(tempDir);
        when(resolveSecretValue.forProject(project.getId(), ExecuteDeployServiceJobUseCase.GIT_TOKEN_SECRET_NAME))
                .thenReturn(Optional.empty());
        doAnswer(inv -> {
                    Files.writeString(
                            tempDir.resolve("atlas.yml"),
                            """
                            apiVersion: atlas/v1alpha1
                            kind: Project
                            runtime:
                              kind: podman-compose
                              composeFile: compose.yml
                            """);
                    return null;
                })
                .when(gitRepository)
                .cloneOrUpdate(eq(service.getRepositoryUrl()), eq("main"), eq(tempDir), eq(Optional.empty()), any());

        assertEquals(RuntimeCapability.PODMAN, useCase.execute(service));
    }

    @Test
    void returnsComposeWhenNoManifest() throws Exception {
        Project project = Project.create("demo", "d");
        ServiceUnit service = ServiceUnit.createDefault(
                project.getId(), "https://git.example/demo.git", "main", "./docker-compose.yml", "");
        when(workspacePathResolver.resolve(service.getId())).thenReturn(tempDir);
        when(resolveSecretValue.forProject(project.getId(), ExecuteDeployServiceJobUseCase.GIT_TOKEN_SECRET_NAME))
                .thenReturn(Optional.empty());
        doAnswer(inv -> null)
                .when(gitRepository)
                .cloneOrUpdate(any(), any(), any(), any(), any());

        assertEquals(RuntimeCapability.COMPOSE, useCase.execute(service));
    }

    @Test
    void softFallsBackToComposeWhenGitFails() {
        Project project = Project.create("demo", "d");
        ServiceUnit service = ServiceUnit.createDefault(
                project.getId(), "https://git.example/demo.git", "main", "./docker-compose.yml", "");
        when(workspacePathResolver.resolve(service.getId())).thenReturn(tempDir);
        when(resolveSecretValue.forProject(project.getId(), ExecuteDeployServiceJobUseCase.GIT_TOKEN_SECRET_NAME))
                .thenReturn(Optional.empty());
        doThrow(new RuntimeException("clone failed"))
                .when(gitRepository)
                .cloneOrUpdate(any(), any(), any(), any(), any());

        assertEquals(RuntimeCapability.COMPOSE, useCase.execute(service));
        verify(gitRepository).cloneOrUpdate(any(), any(), any(), any(), any());
    }
}
