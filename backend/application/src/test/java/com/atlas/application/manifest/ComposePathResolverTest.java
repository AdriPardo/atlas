package com.atlas.application.manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlas.domain.shared.DomainException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ComposePathResolverTest {

    @TempDir
    Path workspace;

    private final ComposePathResolver resolver = new ComposePathResolver();

    @Test
    void fallsBackToComposePathWhenNoManifest() {
        ComposePathResolver.Resolution resolution =
                resolver.resolve(workspace, "docker-compose.yml");

        assertEquals("docker-compose.yml", resolution.composeFilePath());
        assertEquals(ComposePathResolver.Source.COMPOSE_PATH, resolution.source());
        assertEquals(Optional.empty(), resolution.manifestFileName());
        assertTrue(resolution.describe().contains("composePath"));
    }

    @Test
    void usesComposeFileFromManifest() throws Exception {
        Files.writeString(
                workspace.resolve("atlas.yml"),
                """
                apiVersion: atlas/v1alpha1
                kind: Project
                runtime:
                  kind: compose
                  composeFile: docker-compose.atlas.yml
                """);

        ComposePathResolver.Resolution resolution =
                resolver.resolve(workspace, "docker-compose.yml");

        assertEquals("docker-compose.atlas.yml", resolution.composeFilePath());
        assertEquals(ComposePathResolver.Source.MANIFEST, resolution.source());
        assertEquals(Optional.of("atlas.yml"), resolution.manifestFileName());
        assertTrue(resolution.describe().contains("atlas.yml"));
    }

    @Test
    void usesManifestWhenComposePathBlank() throws Exception {
        Files.writeString(
                workspace.resolve("atlas.yml"),
                """
                apiVersion: atlas/v1alpha1
                kind: Project
                runtime:
                  kind: compose
                  composeFile: from-manifest.yml
                """);

        ComposePathResolver.Resolution resolution = resolver.resolve(workspace, null);

        assertEquals("from-manifest.yml", resolution.composeFilePath());
        assertEquals(ComposePathResolver.Source.MANIFEST, resolution.source());
    }

    @Test
    void fallsBackWhenManifestOmitsComposeFile() throws Exception {
        Files.writeString(
                workspace.resolve("atlas.yml"),
                """
                apiVersion: atlas/v1alpha1
                kind: Project
                runtime:
                  kind: compose
                """);

        ComposePathResolver.Resolution resolution =
                resolver.resolve(workspace, "legacy-compose.yml");

        assertEquals("legacy-compose.yml", resolution.composeFilePath());
        assertEquals(ComposePathResolver.Source.COMPOSE_PATH, resolution.source());
        assertEquals(Optional.of("atlas.yml"), resolution.manifestFileName());
    }

    @Test
    void rejectsWhenNeitherManifestComposeFileNorComposePath() throws Exception {
        Files.writeString(
                workspace.resolve("atlas.yml"),
                """
                apiVersion: atlas/v1alpha1
                kind: Project
                runtime:
                  kind: compose
                """);

        DomainException ex =
                assertThrows(DomainException.class, () -> resolver.resolve(workspace, "  "));
        assertTrue(ex.getMessage().contains("Cannot resolve compose file"));
        assertTrue(ex.getMessage().contains("without runtime.composeFile"));
    }

    @Test
    void rejectsWhenNoManifestAndBlankComposePath() {
        DomainException ex =
                assertThrows(DomainException.class, () -> resolver.resolve(workspace, null));
        assertTrue(ex.getMessage().contains("Cannot resolve compose file"));
        assertTrue(ex.getMessage().contains("atlas.yml"));
    }

    @Test
    void rejectsUnsupportedRuntimeKind() throws Exception {
        Files.writeString(
                workspace.resolve("atlas.yml"),
                """
                apiVersion: atlas/v1alpha1
                kind: Project
                runtime:
                  kind: kubernetes
                  composeFile: ignored.yml
                """);

        DomainException ex =
                assertThrows(DomainException.class, () -> resolver.resolve(workspace, "docker-compose.yml"));
        assertTrue(ex.getMessage().contains("Unsupported runtime.kind"));
    }

    @Test
    void treatsOmittedRuntimeKindAsComposeCompatible() throws Exception {
        Files.writeString(
                workspace.resolve("atlas.yml"),
                """
                apiVersion: atlas/v1alpha1
                kind: Project
                runtime:
                  composeFile: only-file.yml
                """);

        ComposePathResolver.Resolution resolution =
                resolver.resolve(workspace, "docker-compose.yml");

        assertEquals("only-file.yml", resolution.composeFilePath());
        assertEquals(ComposePathResolver.Source.MANIFEST, resolution.source());
    }
}
