package com.atlas.application.deployment;

import com.atlas.application.manifest.ComposePathResolver;
import com.atlas.domain.deployment.MigrationStrategy;
import com.atlas.domain.manifest.RuntimeMigrationSpec;
import com.atlas.domain.runtime.RuntimeCapability;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.domain.shared.DomainException;
import java.util.Optional;

/**
 * Resolves the effective post-deploy migration shell command from platform service config and repo
 * manifest ({@code runtime.migration} or legacy {@code runtime.migrateCommand}).
 */
public final class DeployMigrationResolver {

    public record ResolvedMigration(String shellCommand, String source) {}

    public Optional<ResolvedMigration> resolve(
            ServiceUnit service, ComposePathResolver.Resolution compose) {
        if (Boolean.FALSE.equals(service.getMigrationEnabled())) {
            return Optional.empty();
        }

        if (Boolean.TRUE.equals(service.getMigrationEnabled())) {
            return Optional.of(buildFromService(service, compose));
        }

        if (compose.runtimeMigration().isPresent()) {
            RuntimeMigrationSpec spec = compose.runtimeMigration().get();
            if (!spec.isEnabled()) {
                return Optional.empty();
            }
            return Optional.of(buildFromManifest(spec, compose, "runtime.migration"));
        }

        if (compose.migrateCommand().isPresent()) {
            return Optional.of(new ResolvedMigration(
                    compose.migrateCommand().orElseThrow(), "runtime.migrateCommand"));
        }

        return Optional.empty();
    }

    private static ResolvedMigration buildFromService(
            ServiceUnit service, ComposePathResolver.Resolution compose) {
        MigrationStrategy strategy = MigrationStrategy.fromWire(service.getMigrationStrategy());
        String inner = resolveInnerCommand(strategy, service.getMigrationCommand());
        String container = blankToNull(service.getMigrationContainer());
        if (container == null) {
            container = RuntimeMigrationSpec.DEFAULT_CONTAINER;
        }
        String shell = wrapIfContainer(
                compose.composeFilePath(), compose.runtimeCapability(), container, inner);
        return new ResolvedMigration(shell, "service migration config");
    }

    private static ResolvedMigration buildFromManifest(
            RuntimeMigrationSpec spec,
            ComposePathResolver.Resolution compose,
            String source) {
        String inner = resolveInnerCommand(spec.getStrategy(), spec.getCommand().orElse(null));
        String container = spec.getContainer().orElse(RuntimeMigrationSpec.DEFAULT_CONTAINER);
        String shell = wrapIfContainer(
                compose.composeFilePath(), compose.runtimeCapability(), container, inner);
        return new ResolvedMigration(shell, source);
    }

    static String resolveInnerCommand(MigrationStrategy strategy, String commandOverride) {
        if (commandOverride != null && !commandOverride.isBlank()) {
            return commandOverride.trim();
        }
        return strategy
                .defaultInnerCommand()
                .orElseThrow(() -> new DomainException(
                        "migration command is required for strategy " + strategy.wire()));
    }

    static String wrapIfContainer(
            String composeFile,
            RuntimeCapability capability,
            String container,
            String innerCommand) {
        if (container == null || container.isBlank()) {
            return innerCommand;
        }
        String runtime = capability == RuntimeCapability.PODMAN ? "podman compose" : "docker compose";
        return runtime
                + " -f "
                + shellQuote(composeFile)
                + " exec -T "
                + container.trim()
                + " sh -c "
                + shellQuote(innerCommand);
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
