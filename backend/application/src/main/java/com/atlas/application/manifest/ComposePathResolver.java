package com.atlas.application.manifest;

import com.atlas.domain.manifest.ProjectManifest;
import com.atlas.domain.shared.DomainException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Resolves the Compose file for deploy: {@code atlas.yml} {@code runtime.composeFile} when present,
 * else legacy {@code Service.composePath} via an in-memory synthesized manifest (ADR-0014 phase C).
 * Also surfaces optional {@code runtime.migrateCommand} and PUBLIC hardening flags (ADR-0016).
 */
public final class ComposePathResolver {

    public enum Source {
        MANIFEST,
        COMPOSE_PATH
    }

    public record Resolution(
            String composeFilePath,
            Source source,
            Optional<String> manifestFileName,
            Optional<String> migrateCommand,
            boolean minifyEnabled,
            boolean requireTlsEnabled) {

        public Resolution {
            if (manifestFileName == null) {
                manifestFileName = Optional.empty();
            }
            if (migrateCommand == null) {
                migrateCommand = Optional.empty();
            }
        }

        public String describe() {
            if (source == Source.MANIFEST) {
                return "compose file from " + manifestFileName.orElse("atlas.yml") + ": " + composeFilePath;
            }
            return "compose file from composePath: " + composeFilePath;
        }
    }

    private static final String MISSING_COMPOSE_MSG =
            "Cannot resolve compose file: set Service.composePath or add atlas.yml with runtime.composeFile";

    private final ProjectManifestLoader loader;

    public ComposePathResolver() {
        this(new ProjectManifestLoader());
    }

    public ComposePathResolver(ProjectManifestLoader loader) {
        this.loader = loader;
    }

    public Resolution resolve(Path workspace, String composePathFallback) {
        String fallback = blankToNull(composePathFallback);
        Optional<ProjectManifest> loaded = loader.load(workspace);

        if (loaded.isPresent()) {
            ProjectManifest manifest = loaded.get();
            if (!manifest.isComposeCompatible()) {
                throw new DomainException(
                        "Unsupported runtime.kind in "
                                + manifest.getSourceFileName()
                                + ": "
                                + manifest.getRuntimeKind().orElse("(empty)")
                                + " (supports compose / podman-compose only)");
            }

            Optional<String> migrate = manifest.getMigrateCommand();
            Optional<String> fromManifest = manifest.getComposeFile();
            if (fromManifest.isPresent()) {
                return new Resolution(
                        fromManifest.get(),
                        Source.MANIFEST,
                        Optional.of(manifest.getSourceFileName()),
                        migrate,
                        manifest.isMinifyEnabled(),
                        manifest.isRequireTlsEnabled());
            }
            if (fallback != null) {
                return new Resolution(
                        fallback,
                        Source.COMPOSE_PATH,
                        Optional.of(manifest.getSourceFileName()),
                        migrate,
                        manifest.isMinifyEnabled(),
                        manifest.isRequireTlsEnabled());
            }
            throw new DomainException(
                    MISSING_COMPOSE_MSG
                            + " (found "
                            + manifest.getSourceFileName()
                            + " without runtime.composeFile)");
        }

        if (fallback != null) {
            ProjectManifest synthesized = ProjectManifest.synthesizeFromComposePath(fallback);
            return new Resolution(
                    synthesized.getComposeFile().orElseThrow(),
                    Source.COMPOSE_PATH,
                    Optional.empty(),
                    Optional.empty(),
                    synthesized.isMinifyEnabled(),
                    synthesized.isRequireTlsEnabled());
        }

        throw new DomainException(MISSING_COMPOSE_MSG);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
