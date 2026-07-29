package com.atlas.application.manifest;

import com.atlas.domain.manifest.ProjectManifest;
import com.atlas.domain.shared.DomainException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Resolves the Compose file for deploy: {@code atlas.yml} {@code runtime.composeFile} when present,
 * else legacy {@code Service.composePath} (ADR-0014 phase B).
 */
public final class ComposePathResolver {

    public enum Source {
        MANIFEST,
        COMPOSE_PATH
    }

    public record Resolution(String composeFilePath, Source source, Optional<String> manifestFileName) {
        public String describe() {
            if (source == Source.MANIFEST) {
                return "compose file from " + manifestFileName.orElse("atlas.yml") + ": " + composeFilePath;
            }
            return "compose file from composePath: " + composeFilePath;
        }
    }

    private final ProjectManifestLoader loader;

    public ComposePathResolver() {
        this(new ProjectManifestLoader());
    }

    public ComposePathResolver(ProjectManifestLoader loader) {
        this.loader = loader;
    }

    public Resolution resolve(Path workspace, String composePathFallback) {
        if (composePathFallback == null || composePathFallback.isBlank()) {
            throw new DomainException("composePath fallback is required");
        }
        String fallback = composePathFallback.trim();
        Optional<ProjectManifest> loaded = loader.load(workspace);
        if (loaded.isEmpty()) {
            return new Resolution(fallback, Source.COMPOSE_PATH, Optional.empty());
        }

        ProjectManifest manifest = loaded.get();
        if (!manifest.isComposeCompatible()) {
            throw new DomainException(
                    "Unsupported runtime.kind in "
                            + manifest.getSourceFileName()
                            + ": "
                            + manifest.getRuntimeKind().orElse("(empty)")
                            + " (phase B supports compose / podman-compose only)");
        }

        Optional<String> fromManifest = manifest.getComposeFile();
        if (fromManifest.isPresent()) {
            return new Resolution(
                    fromManifest.get(), Source.MANIFEST, Optional.of(manifest.getSourceFileName()));
        }
        return new Resolution(
                fallback, Source.COMPOSE_PATH, Optional.of(manifest.getSourceFileName()));
    }
}
