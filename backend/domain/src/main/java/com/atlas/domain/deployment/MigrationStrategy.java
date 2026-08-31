package com.atlas.domain.deployment;

import com.atlas.domain.shared.DomainException;
import java.util.Locale;
import java.util.Optional;

/** App-owned migrator hint. Atlas only builds a shell command; it does not run ORM tools itself. */
public enum MigrationStrategy {
    PRISMA("prisma", "npm run migrate:deploy"),
    FLYWAY("flyway", "flyway migrate"),
    CUSTOM("custom", null);

    private final String wire;
    private final String defaultInnerCommand;

    MigrationStrategy(String wire, String defaultInnerCommand) {
        this.wire = wire;
        this.defaultInnerCommand = defaultInnerCommand;
    }

    public String wire() {
        return wire;
    }

    public Optional<String> defaultInnerCommand() {
        return Optional.ofNullable(defaultInnerCommand);
    }

    public static MigrationStrategy fromWire(String value) {
        if (value == null || value.isBlank()) {
            return CUSTOM;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (MigrationStrategy strategy : values()) {
            if (strategy.wire.equals(normalized)) {
                return strategy;
            }
        }
        throw new DomainException(
                "Unsupported migration strategy: " + value + " (expected prisma, flyway, or custom)");
    }
}
