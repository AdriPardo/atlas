package com.atlas.domain.manifest;

import java.util.Locale;
import java.util.Optional;

/**
 * Minimal project manifest (ADR-0014). Repo declares how to run; Compose is today's adapter.
 */
public final class ProjectManifest {

    public static final String API_VERSION_V1_ALPHA1 = "atlas/v1alpha1";
    public static final String KIND_PROJECT = "Project";

    private final String apiVersion;
    private final String kind;
    private final String runtimeKind;
    private final String composeFile;
    private final String sourceFileName;

    public static final String SYNTHESIZED_SOURCE = "(synthesized)";

    public ProjectManifest(
            String apiVersion, String kind, String runtimeKind, String composeFile, String sourceFileName) {
        this.apiVersion = requireText(apiVersion, "apiVersion");
        this.kind = requireText(kind, "kind");
        this.runtimeKind = blankToNull(runtimeKind);
        this.composeFile = blankToNull(composeFile);
        this.sourceFileName = requireText(sourceFileName, "sourceFileName");
    }

    /**
     * In-memory minimal manifest for compose-only repos without {@code atlas.yml}
     * (ADR-0014 phase C).
     */
    public static ProjectManifest synthesizeFromComposePath(String composePath) {
        String path = requireText(composePath, "composePath");
        return new ProjectManifest(API_VERSION_V1_ALPHA1, KIND_PROJECT, "compose", path, SYNTHESIZED_SOURCE);
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
