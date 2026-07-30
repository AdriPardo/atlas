package com.atlas.domain.database;

import com.atlas.domain.shared.DomainException;
import java.util.Locale;

/**
 * Logical Atlas DB profiles (ADR-0015) mapped to Postgres grants on issue of TTL credentials.
 */
public enum DatabaseAccessProfile {
    READ("db.read"),
    MIGRATE("db.migrate"),
    ADMIN("db.admin");

    private final String wire;

    DatabaseAccessProfile(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static DatabaseAccessProfile fromWire(String raw) {
        if (raw == null || raw.isBlank()) {
            return READ;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (DatabaseAccessProfile profile : values()) {
            if (profile.wire.equals(normalized) || profile.name().equalsIgnoreCase(normalized)) {
                return profile;
            }
        }
        throw new DomainException("Unknown database access profile: " + raw);
    }
}
