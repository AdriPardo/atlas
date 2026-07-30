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
    public static final String READ_PROFILE = "db.read";
    public static final String ADMIN_PROFILE = "db.admin";

    /** Default TTL for human console credentials (option C). */
    public static final int DEFAULT_CREDENTIAL_TTL_MINUTES = 60;
    public static final int MIN_CREDENTIAL_TTL_MINUTES = 5;
    public static final int MAX_CREDENTIAL_TTL_MINUTES = 24 * 60;

    private static final int PG_IDENT_MAX = 63;

    private ProjectDatabaseNames() {}

    public static String schemaName(String projectSlug) {
        String base = sanitizeSlug(projectSlug);
        return truncate("app_" + base);
    }

    public static String migratorRole(String projectSlug) {
        return withSuffix(schemaName(projectSlug), "_migrator");
    }

    /** Persistent read-only role ({@code app_<slug>_ro}) used as grant source for TTL {@code db.read}. */
    public static String readOnlyRole(String projectSlug) {
        return withSuffix(schemaName(projectSlug), "_ro");
    }

    /**
     * Ephemeral TTL login role: {@code app_<slug>_t_<suffix>} (suffix = short random token).
     * Never reuses migrator password stored in {@code db.url}.
     */
    public static String temporaryCredentialRole(String projectSlug, String suffix) {
        if (suffix == null || !suffix.matches("[a-z0-9]{4,16}")) {
            throw new DomainException("credential role suffix must be 4–16 [a-z0-9]");
        }
        return withSuffix(schemaName(projectSlug), "_t_" + suffix);
    }

    /** Prefix used to list/revoke only this project's ephemeral credential roles. */
    public static String temporaryCredentialRolePrefix(String projectSlug) {
        String schema = schemaName(projectSlug);
        String prefix = schema + "_t_";
        if (prefix.length() <= PG_IDENT_MAX) {
            return prefix;
        }
        return truncate(schema.substring(0, Math.max(1, PG_IDENT_MAX - 3))) + "_t_";
    }

    public static int clampTtlMinutes(Integer ttlMinutes) {
        int value = ttlMinutes == null ? DEFAULT_CREDENTIAL_TTL_MINUTES : ttlMinutes;
        if (value < MIN_CREDENTIAL_TTL_MINUTES || value > MAX_CREDENTIAL_TTL_MINUTES) {
            throw new DomainException(
                    "ttlMinutes must be between "
                            + MIN_CREDENTIAL_TTL_MINUTES
                            + " and "
                            + MAX_CREDENTIAL_TTL_MINUTES);
        }
        return value;
    }

    private static String withSuffix(String schema, String suffix) {
        String role = schema + suffix;
        if (role.length() <= PG_IDENT_MAX) {
            return role;
        }
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
