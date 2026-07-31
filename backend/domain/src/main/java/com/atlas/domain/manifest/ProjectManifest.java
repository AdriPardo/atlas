package com.atlas.domain.manifest;

import com.atlas.domain.runtime.RuntimeCapability;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Minimal project manifest (ADR-0014). Repo declares how to run; Compose is today's adapter.
 * PUBLIC hardening hints (ADR-0016): {@code build.minify}, {@code exposure.requireTls}.
 * Secrets: {@code envFrom.secretRef} → workspace {@code .env} on deploy (ADR-0015 delivery).
 */
public final class ProjectManifest {

    public static final String API_VERSION_V1_ALPHA1 = "atlas/v1alpha1";
    public static final String KIND_PROJECT = "Project";

    private final String apiVersion;
    private final String kind;
    private final String runtimeKind;
    private final String composeFile;
    private final String migrateCommand;
    private final Boolean minify;
    private final Boolean requireTls;
    private final List<EnvFromSecretRef> envFromSecrets;
    private final String sourceFileName;

    public static final String SYNTHESIZED_SOURCE = "(synthesized)";

    public ProjectManifest(
            String apiVersion,
            String kind,
            String runtimeKind,
            String composeFile,
            String migrateCommand,
            String sourceFileName) {
        this(apiVersion, kind, runtimeKind, composeFile, migrateCommand, null, null, List.of(), sourceFileName);
    }

    public ProjectManifest(
            String apiVersion,
            String kind,
            String runtimeKind,
            String composeFile,
            String migrateCommand,
            Boolean minify,
            Boolean requireTls,
            String sourceFileName) {
        this(apiVersion, kind, runtimeKind, composeFile, migrateCommand, minify, requireTls, List.of(), sourceFileName);
    }

    public ProjectManifest(
            String apiVersion,
            String kind,
            String runtimeKind,
            String composeFile,
            String migrateCommand,
            Boolean minify,
            Boolean requireTls,
            List<EnvFromSecretRef> envFromSecrets,
            String sourceFileName) {
        this.apiVersion = requireText(apiVersion, "apiVersion");
        this.kind = requireText(kind, "kind");
        this.runtimeKind = blankToNull(runtimeKind);
        this.composeFile = blankToNull(composeFile);
        this.migrateCommand = blankToNull(migrateCommand);
        this.minify = minify;
        this.requireTls = requireTls;
        this.envFromSecrets = envFromSecrets == null || envFromSecrets.isEmpty()
                ? List.of()
                : List.copyOf(envFromSecrets);
        this.sourceFileName = requireText(sourceFileName, "sourceFileName");
    }

    /**
     * In-memory minimal manifest for compose-only repos without {@code atlas.yml}
     * (ADR-0014 phase C).
     */
    public static ProjectManifest synthesizeFromComposePath(String composePath) {
        String path = requireText(composePath, "composePath");
        return new ProjectManifest(
                API_VERSION_V1_ALPHA1, KIND_PROJECT, "compose", path, null, null, null, List.of(), SYNTHESIZED_SOURCE);
    }

    public boolean isSynthesized() {
        return SYNTHESIZED_SOURCE.equals(sourceFileName);
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getKind() {
        return kind;
    }

    public Optional<String> getRuntimeKind() {
        return Optional.ofNullable(runtimeKind);
    }

    public Optional<String> getComposeFile() {
        return Optional.ofNullable(composeFile);
    }

    /**
     * Optional shell command run by the deploy job after stack apply.
     * App owns the migrator; Atlas only invokes the declared string.
     */
    public Optional<String> getMigrateCommand() {
        return Optional.ofNullable(migrateCommand);
    }

    /**
     * Declared {@code build.minify}; empty means platform default (true).
     */
    public Optional<Boolean> getMinify() {
        return Optional.ofNullable(minify);
    }

    /**
     * Declared {@code exposure.requireTls}; empty means platform default (true).
     */
    public Optional<Boolean> getRequireTls() {
        return Optional.ofNullable(requireTls);
    }

    /** ADR-0016: production frontend build unless explicitly disabled. */
    public boolean isMinifyEnabled() {
        return minify == null || minify;
    }

    /** ADR-0016: PUBLIC edge must use TLS unless explicitly disabled. */
    public boolean isRequireTlsEnabled() {
        return requireTls == null || requireTls;
    }

    /**
     * Logical secrets to inject into workspace {@code .env} before compose apply.
     * Empty when omitted or synthesized.
     */
    public List<EnvFromSecretRef> getEnvFromSecrets() {
        return envFromSecrets;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    /** Compose / podman-compose (or omitted kind) are the only runtimes supported in phase B/C. */
    public boolean isComposeCompatible() {
        if (runtimeKind == null) {
            return true;
        }
        String normalized = runtimeKind.toLowerCase(Locale.ROOT);
        return "compose".equals(normalized) || "podman-compose".equals(normalized);
    }

    /**
     * Host capability required for deploy. Opt-in: {@code runtime.kind: podman-compose} → {@link
     * RuntimeCapability#PODMAN}; omitted / {@code compose} → {@link RuntimeCapability#COMPOSE}.
     */
    public RuntimeCapability requiredRuntimeCapability() {
        if (runtimeKind != null && "podman-compose".equals(runtimeKind.toLowerCase(Locale.ROOT))) {
            return RuntimeCapability.PODMAN;
        }
        return RuntimeCapability.COMPOSE;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
