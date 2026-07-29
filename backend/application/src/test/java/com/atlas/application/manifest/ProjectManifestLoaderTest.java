package com.atlas.application.manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlas.domain.manifest.ProjectManifest;
import com.atlas.domain.shared.DomainException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectManifestLoaderTest {

    @TempDir
    Path workspace;

    private final ProjectManifestLoader loader = new ProjectManifestLoader();

    @Test
    void returnsEmptyWhenNoManifestFile() {
        assertEquals(Optional.empty(), loader.load(workspace));
    }

    @Test
    void loadsCanonicalAtlasYml() throws Exception {
        Files.writeString(
                workspace.resolve("atlas.yml"),
                """
                apiVersion: atlas/v1alpha1
                kind: Project
                runtime:
                  kind: compose
                  composeFile: docker-compose.atlas.yml
                """);

        ProjectManifest manifest = loader.load(workspace).orElseThrow();

        assertEquals("atlas/v1alpha1", manifest.getApiVersion());
        assertEquals("Project", manifest.getKind());
        assertEquals(Optional.of("compose"), manifest.getRuntimeKind());
        assertEquals(Optional.of("docker-compose.atlas.yml"), manifest.getComposeFile());
        assertEquals("atlas.yml", manifest.getSourceFileName());
        assertTrue(manifest.isComposeCompatible());
    }

    @Test
    void prefersAtlasYmlOverAlias() throws Exception {
        Files.writeString(
                workspace.resolve("atlas.yml"),
                """
                apiVersion: atlas/v1alpha1
                kind: Project
                runtime:
                  composeFile: from-canonical.yml
                """);
        Files.writeString(
                workspace.resolve("atlas.project.yml"),
                """
                apiVersion: atlas/v1alpha1
                kind: Project
                runtime:
                  composeFile: from-alias.yml
                """);

        assertEquals(
                Optional.of("from-canonical.yml"),
                loader.load(workspace).flatMap(ProjectManifest::getComposeFile));
    }

    @Test
    void loadsAliasWhenCanonicalMissing() throws Exception {
        Files.writeString(
                workspace.resolve("atlas.project.yml"),
                """
                apiVersion: atlas/v1alpha1
                kind: Project
                runtime:
                  kind: compose
                  composeFile: alias-compose.yml
                """);

        ProjectManifest manifest = loader.load(workspace).orElseThrow();
        assertEquals("atlas.project.yml", manifest.getSourceFileName());
        assertEquals(Optional.of("alias-compose.yml"), manifest.getComposeFile());
    }

    @Test
    void rejectsMissingApiVersion() throws Exception {
        Files.writeString(
                workspace.resolve("atlas.yml"),
                """
                kind: Project
                runtime:
                  composeFile: docker-compose.yml
                """);

        DomainException ex = assertThrows(DomainException.class, () -> loader.load(workspace));
        assertTrue(ex.getMessage().contains("apiVersion"));
    }

    @Test
    void rejectsUnsupportedKind() throws Exception {
        Files.writeString(
                workspace.resolve("atlas.yml"),
                """
                apiVersion: atlas/v1alpha1
                kind: Service
                """);

        DomainException ex = assertThrows(DomainException.class, () -> loader.load(workspace));
        assertTrue(ex.getMessage().contains("kind"));
    }
}
