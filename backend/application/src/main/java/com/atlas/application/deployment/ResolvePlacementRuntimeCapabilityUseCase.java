package com.atlas.application.deployment;

import com.atlas.application.manifest.ProjectManifestLoader;
import com.atlas.application.port.out.GitRepositoryPort;
import com.atlas.application.secret.ResolveSecretValueUseCase;
import com.atlas.domain.manifest.ProjectManifest;
import com.atlas.domain.runtime.RuntimeCapability;
import com.atlas.domain.service.ServiceUnit;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Peeks {@code atlas.yml} before Autopilot SHARED placement so host selection can filter by the
 * runtime capability the manifest requires (ADR-0014). Soft-falls back to {@link
 * RuntimeCapability#COMPOSE} when the repo cannot be read yet — the deploy job still enforces the
 * real capability after clone.
 */
@Service
@RequiredArgsConstructor
public class ResolvePlacementRuntimeCapabilityUseCase {

    private final GitRepositoryPort gitRepository;
    private final ResolveSecretValueUseCase resolveSecretValue;
    private final PlacementWorkspacePathResolver workspacePathResolver;
    private final ProjectManifestLoader loader = new ProjectManifestLoader();

    public RuntimeCapability execute(ServiceUnit service) {
        if (service == null) {
            return RuntimeCapability.COMPOSE;
        }
        String repo = service.getRepositoryUrl();
        if (repo == null || repo.isBlank()) {
            return RuntimeCapability.COMPOSE;
        }
        Path workspace = workspacePathResolver.resolve(service.getId());
        try {
            Optional<String> token = resolveSecretValue.forProject(
                    service.getProjectId(), ExecuteDeployServiceJobUseCase.GIT_TOKEN_SECRET_NAME);
            gitRepository.cloneOrUpdate(repo, service.getBranch(), workspace, token, line -> {});
            return loader
                    .load(workspace)
                    .map(ProjectManifest::requiredRuntimeCapability)
                    .orElse(RuntimeCapability.COMPOSE);
        } catch (RuntimeException ignored) {
            // Peek is best-effort; deploy job re-validates capability after the real clone.
            return RuntimeCapability.COMPOSE;
        }
    }

    @FunctionalInterface
    public interface PlacementWorkspacePathResolver {
        Path resolve(UUID serviceId);
    }
}
