package com.atlas.domain.database;

import com.atlas.domain.shared.DomainException;
import java.util.Locale;
import java.util.Objects;

/**
 * ADR-0015 naming: schema {@code app_<slug>} and migrator role {@code app_<slug>_migrator}.
 * Slug hyphens become underscores; identifiers stay within Postgres' 63-char limit.
 */
public final class ProjectDatabaseNames {

    public static final String CONTROL_PLANE_DB = "atlas";
    public static final String DB_URL_SECRET = "db.url";
    public static final String DB_SCHEMA_SECRET = "db.schema";
    public static final String DEFAULT_PROFILE = "db.migrate";

    private static final int PG_IDENT_MAX = 63;

    private ProjectDatabaseNames() {}

    public static String schemaName(String projectSlug) {
        String base = sanitizeSlug(projectSlug);
        return truncate("app_" + base);
    }

    public static String migratorRole(String projectSlug) {
        String schema = schemaName(projectSlug);
        String role = schema + "_migrator";
        if (role.length() <= PG_IDENT_MAX) {
            return role;
        }
        // Keep suffix; trim middle of schema prefix.
        String suffix = "_migrator";
        return truncate(schema.substring(0, PG_IDENT_MAX - suffix.length())) + suffix;
    }

    public static void rejectControlPlaneDatabase(String databaseName) {
        if (databaseName == null || databaseName.isBlank()) {
            throw new DomainException("app database name is required");
        }
        if (CONTROL_PLANE_DB.equalsIgnoreCase(databaseName.trim())) {
            throw new DomainException(
                    "Refusing to provision into control-plane database '"
                            + CONTROL_PLANE_DB
                            + "' — configure ATLAS_APP_DB_URL to a dedicated apps database");
        }
    }

    private static String sanitizeSlug(String projectSlug) {
        Objects.requireNonNull(projectSlug, "projectSlug");
        String base = projectSlug
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replaceAll("[^a-z0-9_]", "")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (base.isBlank()) {
            throw new DomainException("project slug must yield a non-empty database identifier");
        }
        return base;
    }

    private static String truncate(String ident) {
        if (ident.length() <= PG_IDENT_MAX) {
            return ident;
        }
        return ident.substring(0, PG_IDENT_MAX).replaceAll("_+$", "");
    }
}
