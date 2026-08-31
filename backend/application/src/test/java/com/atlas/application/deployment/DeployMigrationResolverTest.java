package com.atlas.application.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlas.application.manifest.ComposePathResolver;
import com.atlas.domain.deployment.MigrationStrategy;
import com.atlas.domain.manifest.RuntimeMigrationSpec;
import com.atlas.domain.runtime.RuntimeCapability;
import com.atlas.domain.service.ServiceExposure;
import com.atlas.domain.service.ServiceStatus;
import com.atlas.domain.service.ServiceUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeployMigrationResolverTest {

    private final DeployMigrationResolver resolver = new DeployMigrationResolver();

    @Test
    void buildsDockerExecForStructuredManifestMigration() {
        ServiceUnit service = serviceWithMigration(null, null, null, null);
        ComposePathResolver.Resolution compose = resolution(
                Optional.of(new RuntimeMigrationSpec(
                        true,
                        MigrationStrategy.PRISMA,
                        "npm run migrate:deploy -w @autotube/database",
                        "api")),
                Optional.empty());

        DeployMigrationResolver.ResolvedMigration resolved =
                resolver.resolve(service, compose).orElseThrow();

        assertEquals(
                "docker compose -f 'docker-compose.atlas.yml' exec -T api sh -c 'npm run migrate:deploy -w @autotube/database'",
                resolved.shellCommand());
        assertEquals("runtime.migration", resolved.source());
    }

    @Test
    void serviceOverrideWrapsPrismaInApiContainer() {
        ServiceUnit service = serviceWithMigration(
                true, "prisma", "npm run migrate:deploy -w @autotube/database", "api");
        ComposePathResolver.Resolution compose = resolution(Optional.empty(), Optional.empty());

        String shell = resolver.resolve(service, compose).orElseThrow().shellCommand();

        assertTrue(shell.contains("exec -T api"));
        assertTrue(shell.contains("migrate:deploy -w @autotube/database"));
    }

    @Test
    void serviceMigrationDisabledSkipsManifestHook() {
        ServiceUnit service = serviceWithMigration(false, null, null, null);
        ComposePathResolver.Resolution compose =
                resolution(Optional.empty(), Optional.of("npm run db:migrate:deploy"));

        assertTrue(resolver.resolve(service, compose).isEmpty());
    }

    @Test
    void legacyMigrateCommandRunsAsHostShell() {
        ServiceUnit service = serviceWithMigration(null, null, null, null);
        ComposePathResolver.Resolution compose =
                resolution(Optional.empty(), Optional.of("npm run db:migrate:deploy"));

        assertEquals(
                "npm run db:migrate:deploy",
                resolver.resolve(service, compose).orElseThrow().shellCommand());
    }

    @Test
    void podmanRuntimeUsesPodmanComposeBinary() {
        ServiceUnit service = serviceWithMigration(null, null, null, null);
        ComposePathResolver.Resolution compose = new ComposePathResolver.Resolution(
                "docker-compose.atlas.yml",
                ComposePathResolver.Source.MANIFEST,
                Optional.of("atlas.yml"),
                Optional.empty(),
                Optional.of(new RuntimeMigrationSpec(true, MigrationStrategy.FLYWAY, null, "api")),
                true,
                true,
                java.util.List.of(),
                RuntimeCapability.PODMAN);

        String shell = resolver.resolve(service, compose).orElseThrow().shellCommand();

        assertTrue(shell.startsWith("podman compose"));
        assertTrue(shell.contains("flyway migrate"));
    }

    private static ServiceUnit serviceWithMigration(
            Boolean enabled, String strategy, String command, String container) {
        return ServiceUnit.rehydrate(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "default",
                "https://example.com/repo.git",
                "main",
                "",
                "",
                "default",
                ServiceExposure.PUBLIC,
                ServiceStatus.REGISTERED,
                enabled,
                strategy,
                command,
                container,
                java.time.Instant.now(),
                java.time.Instant.now());
    }

    private static ComposePathResolver.Resolution resolution(
            Optional<RuntimeMigrationSpec> runtimeMigration, Optional<String> migrateCommand) {
        return new ComposePathResolver.Resolution(
                "docker-compose.atlas.yml",
                ComposePathResolver.Source.MANIFEST,
                Optional.of("atlas.yml"),
                migrateCommand,
                runtimeMigration,
                true,
                true,
                java.util.List.of(),
                RuntimeCapability.COMPOSE);
    }
}
