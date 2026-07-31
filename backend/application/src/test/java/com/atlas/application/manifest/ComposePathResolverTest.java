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
        assertEquals(Optional.empty(), resolution.migrateCommand());
        assertTrue(resolution.minifyEnabled());
        assertTrue(resolution.requireTlsEnabled());
        assertTrue(resolution.envFromSecrets().isEmpty());
        assertEquals(com.atlas.domain.runtime.RuntimeCapability.COMPOSE, resolution.runtimeCapability());
        assertTrue(resolution.describe().contains("composePath"));
    }

    @Test
    void surfacesEnvFromSecretsFromManifest() throws Exception {
        Files.writeString(
                workspace.resolve("atlas.yml"),
                """
                apiVersion: atlas/v1alpha1
                kind: Project
                runtime:
                  composeFile: docker-compose.atlas.yml
                  envFrom:
                    - secretRef: db.url
                """);

        ComposePathResolver.Resolution resolution = resolver.resolve(workspace, null);

        assertEquals(1, resolution.envFromSecrets().size());
        assertEquals("DATABASE_URL", resolution.envFromSecrets().get(0).resolveEnvKey());
    }

    @Test
    void surfacesPublicHardeningFlagsFromManifest() throws Exception {
        Files.writeString(
                workspace.resolve("atlas.yml"),
                """
                apiVersion: atlas/v1alpha1
                kind: Project
                runtime:
                  composeFile: docker-compose.atlas.yml
                build:
                  minify: false
                exposure:
                  requireTls: false
                """);

        ComposePathResolver.Resolution resolution = resolver.resolve(workspace, null);

        assertTrue(!resolution.minifyEnabled());
        assertTrue(!resolution.requireTlsEnabled());
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
        assertEquals(Optional.empty(), resolution.migrateCommand());
        assertTrue(resolution.describe().contains("atlas.yml"));
    }

    @Test
    void surfacesMigrateCommandFromManifest() throws Exception {
        Files.writeString(
                workspace.resolve("atlas.yml"),
                """
                apiVersion: atlas/v1alpha1
                kind: Project
                runtime:
                  composeFile: docker-compose.atlas.yml
                  migrateCommand: npm run db:migrate:deploy
                """);

        ComposePathResolver.Resolution resolution = resolver.resolve(workspace, null);

        assertEquals(Optional.of("npm run db:migrate:deploy"), resolution.migrateCommand());
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
        assertEquals(com.atlas.domain.runtime.RuntimeCapability.COMPOSE, resolution.runtimeCapability());
    }

    @Test
    void mapsPodmanComposeKindToPodmanCapability() throws Exception {
        Files.writeString(
                workspace.resolve("atlas.yml"),
                """
                apiVersion: atlas/v1alpha1
                kind: Project
                runtime:
                  kind: podman-compose
                  composeFile: compose.yml
                """);

        ComposePathResolver.Resolution resolution = resolver.resolve(workspace, null);

        assertEquals("compose.yml", resolution.composeFilePath());
        assertEquals(com.atlas.domain.runtime.RuntimeCapability.PODMAN, resolution.runtimeCapability());
        assertTrue(resolution.describe().contains("runtime=podman"));
    }
}
