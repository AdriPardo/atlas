package com.atlas.domain.manifest;

import com.atlas.domain.deployment.MigrationStrategy;
import java.util.Optional;

/**
 * Structured {@code runtime.migration} block in atlas.yml (optional). Legacy {@code migrateCommand}
 * remains supported when this block is absent.
 */
public final class RuntimeMigrationSpec {

    public static final String DEFAULT_CONTAINER = "api";

    private final boolean enabled;
    private final MigrationStrategy strategy;
    private final String command;
    private final String container;

    public RuntimeMigrationSpec(
            boolean enabled, MigrationStrategy strategy, String command, String container) {
        this.enabled = enabled;
        this.strategy = strategy == null ? MigrationStrategy.CUSTOM : strategy;
        this.command = blankToNull(command);
        this.container = blankToNull(container);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public MigrationStrategy getStrategy() {
        return strategy;
    }

    public Optional<String> getCommand() {
        return Optional.ofNullable(command);
    }

    public Optional<String> getContainer() {
        return Optional.ofNullable(container);
    }

    public String resolveContainer() {
        return container == null ? DEFAULT_CONTAINER : container;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
