package com.atlas.infrastructure.database;

import com.atlas.application.port.out.ProjectDatabaseProvisionerPort;
import com.atlas.domain.database.DatabaseAccessProfile;
import com.atlas.domain.database.ProjectDatabaseNames;
import com.atlas.domain.shared.DomainException;
import com.atlas.infrastructure.config.AtlasProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Creates schema + migrator/read roles and issues TTL login roles on the shared apps Postgres
 * database (ADR-0015).
 *
 * <p>Admin credentials come from {@code atlas.app-database.*} / {@code ATLAS_APP_DB_*}. Refuses
 * database name {@code atlas}.
 */
@Component
public class PostgresProjectDatabaseProvisionerAdapter implements ProjectDatabaseProvisionerPort {

    private static final Pattern JDBC_PG =
            Pattern.compile("^jdbc:postgresql://([^/?]+)(/([^/?]*))?.*$", Pattern.CASE_INSENSITIVE);

    private final AtlasProperties properties;

    public PostgresProjectDatabaseProvisionerAdapter(AtlasProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isConfigured() {
        AtlasProperties.AppDatabase cfg = properties.getAppDatabase();
        return blankToNull(cfg.getJdbcUrl()) != null
                && blankToNull(cfg.getUsername()) != null
                && blankToNull(cfg.getPassword()) != null;
    }

    @Override
    public String databaseName() {
        return parseDatabaseName(requireJdbcUrl());
    }

    @Override
    public String hostPort() {
        return parseHostPort(requireJdbcUrl());
    }

    @Override
    public ProvisionResult provision(ProvisionRequest request) {
        if (!isConfigured()) {
            throw new DomainException("Project DB provisioner not configured");
        }
        String jdbcUrl = requireJdbcUrl();
        String dbName = parseDatabaseName(jdbcUrl);
        ProjectDatabaseNames.rejectControlPlaneDatabase(dbName);

        String schema = requireIdent(request.schema(), "schema");
        String role = requireIdent(request.role(), "role");
        String readRole = requireIdent(request.readRole(), "readRole");
        String password = request.password();
        if (password == null || password.isBlank()) {
            throw new DomainException("password is required");
        }

        try (Connection connection = openAdmin(jdbcUrl);
                Statement stmt = connection.createStatement()) {
            connection.setAutoCommit(true);
            ensureRole(stmt, role, password, null, false);
            ensureSchema(stmt, schema, role);
            grantConnect(stmt, dbName, role);
            stmt.execute("ALTER ROLE " + quoteIdent(role) + " SET search_path TO " + quoteIdent(schema));
            ensureReadRole(stmt, dbName, schema, readRole, role);
        } catch (SQLException e) {
            throw new DomainException("Postgres provision failed: " + e.getMessage());
        }

        String connectionUrl = buildAppConnectionUrl(role, password, jdbcUrl, schema);
        return new ProvisionResult(schema, role, readRole, dbName, connectionUrl);
    }

    @Override
    public CredentialResult issueCredential(CredentialRequest request) {
        if (!isConfigured()) {
            throw new DomainException("Project DB provisioner not configured");
        }
        String jdbcUrl = requireJdbcUrl();
        String dbName = parseDatabaseName(jdbcUrl);
        ProjectDatabaseNames.rejectControlPlaneDatabase(dbName);

        String schema = requireIdent(request.schema(), "schema");
        String migratorRole = requireIdent(request.migratorRole(), "migratorRole");
        String readRole = requireIdent(request.readRole(), "readRole");
        String temporaryRole = requireIdent(request.temporaryRole(), "temporaryRole");
        String password = request.password();
        if (password == null || password.isBlank()) {
            throw new DomainException("password is required");
        }
        if (request.validUntil() == null || !request.validUntil().isAfter(Instant.now())) {
            throw new DomainException("validUntil must be in the future");
        }
        DatabaseAccessProfile profile = DatabaseAccessProfile.fromWire(request.profile());

        try (Connection connection = openAdmin(jdbcUrl);
                Statement stmt = connection.createStatement()) {
            connection.setAutoCommit(true);
            requireRoleExists(stmt, migratorRole, "Project schema not provisioned (migrator role missing)");
            requireSchemaExists(stmt, schema);
            ensureReadRole(stmt, dbName, schema, readRole, migratorRole);
            // INHERIT so GRANT migrator/ro membership actually applies at login.
            ensureRole(stmt, temporaryRole, password, request.validUntil(), true);
            grantConnect(stmt, dbName, temporaryRole);
            stmt.execute(
                    "ALTER ROLE " + quoteIdent(temporaryRole) + " SET search_path TO " + quoteIdent(schema));
            applyProfileGrants(stmt, profile, schema, migratorRole, readRole, temporaryRole);
        } catch (SQLException e) {
            throw new DomainException("Postgres credential issue failed: " + e.getMessage());
        }

        return new CredentialResult(
                temporaryRole,
                profile.wire(),
                buildAppConnectionUrl(temporaryRole, password, jdbcUrl, schema),
                request.validUntil());
    }

    @Override
    public void revokeCredential(String role) {
        if (!isConfigured()) {
            throw new DomainException("Project DB provisioner not configured");
        }
        String safeRole = requireIdent(role, "role");
        String jdbcUrl = requireJdbcUrl();
        try (Connection connection = openAdmin(jdbcUrl);
                Statement stmt = connection.createStatement()) {
            connection.setAutoCommit(true);
            // Drop memberships / privileges owned by the ephemeral role, then drop login.
            try {
                stmt.execute("DROP OWNED BY " + quoteIdent(safeRole));
            } catch (SQLException ignored) {
                // Role may not exist yet — DROP ROLE below is idempotent enough via IF EXISTS.
            }
            stmt.execute("DROP ROLE IF EXISTS " + quoteIdent(safeRole));
        } catch (SQLException e) {
            throw new DomainException("Postgres credential revoke failed: " + e.getMessage());
        }
    }

    @Override
    public List<CredentialInfo> listCredentials(String rolePrefix) {
        if (!isConfigured()) {
            return List.of();
        }
        if (rolePrefix == null || rolePrefix.isBlank()) {
            throw new DomainException("rolePrefix is required");
        }
        String prefix = rolePrefix.trim().toLowerCase(Locale.ROOT);
        String jdbcUrl = requireJdbcUrl();
        Instant now = Instant.now();
        List<CredentialInfo> out = new ArrayList<>();
        try (Connection connection = openAdmin(jdbcUrl);
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT rolname, rolvaliduntil FROM pg_roles WHERE rolname LIKE ? ORDER BY rolname")) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString(1);
                    java.sql.Timestamp until = rs.getTimestamp(2);
                    Instant expiresAt = until == null ? null : until.toInstant();
                    boolean expired = expiresAt != null && !expiresAt.isAfter(now);
                    out.add(new CredentialInfo(name, expiresAt, expired));
                }
            }
        } catch (SQLException e) {
            throw new DomainException("Postgres credential list failed: " + e.getMessage());
        }
        return out;
    }

    private void applyProfileGrants(
            Statement stmt,
            DatabaseAccessProfile profile,
            String schema,
            String migratorRole,
            String readRole,
            String temporaryRole)
            throws SQLException {
        switch (profile) {
            case READ -> {
                stmt.execute("GRANT " + quoteIdent(readRole) + " TO " + quoteIdent(temporaryRole));
            }
            case MIGRATE -> {
                stmt.execute("GRANT " + quoteIdent(migratorRole) + " TO " + quoteIdent(temporaryRole));
            }
            case ADMIN -> {
                stmt.execute("GRANT " + quoteIdent(migratorRole) + " TO " + quoteIdent(temporaryRole));
                stmt.execute("GRANT ALL ON SCHEMA " + quoteIdent(schema) + " TO " + quoteIdent(temporaryRole));
                stmt.execute(
                        "GRANT ALL ON ALL TABLES IN SCHEMA " + quoteIdent(schema) + " TO " + quoteIdent(temporaryRole));
                stmt.execute(
                        "GRANT ALL ON ALL SEQUENCES IN SCHEMA "
                                + quoteIdent(schema)
                                + " TO "
                                + quoteIdent(temporaryRole));
            }
        }
    }

    private void ensureReadRole(Statement stmt, String dbName, String schema, String readRole, String ownerRole)
            throws SQLException {
        // Stable NOLOGIN-capable password placeholder; TTL logins inherit via GRANT.
        boolean exists;
        try (ResultSet rs = stmt.executeQuery(
                "SELECT 1 FROM pg_roles WHERE rolname = " + quoteLiteral(readRole))) {
            exists = rs.next();
        }
        if (!exists) {
            // Omit NOSUPERUSER/NOCREATEDB/NOCREATEROLE: PG16+ forbids non-superuser CREATEROLE
            // roles from setting those attributes (defaults already match).
            stmt.execute("CREATE ROLE " + quoteIdent(readRole) + " WITH NOLOGIN");
        }
        grantConnect(stmt, dbName, readRole);
        stmt.execute("GRANT USAGE ON SCHEMA " + quoteIdent(schema) + " TO " + quoteIdent(readRole));
        stmt.execute("GRANT SELECT ON ALL TABLES IN SCHEMA " + quoteIdent(schema) + " TO " + quoteIdent(readRole));
        stmt.execute(
                "GRANT SELECT ON ALL SEQUENCES IN SCHEMA " + quoteIdent(schema) + " TO " + quoteIdent(readRole));
        // Future tables created by migrator: default SELECT for read role.
        stmt.execute(
                "ALTER DEFAULT PRIVILEGES FOR ROLE "
                        + quoteIdent(ownerRole)
                        + " IN SCHEMA "
                        + quoteIdent(schema)
                        + " GRANT SELECT ON TABLES TO "
                        + quoteIdent(readRole));
        stmt.execute(
                "ALTER DEFAULT PRIVILEGES FOR ROLE "
                        + quoteIdent(ownerRole)
                        + " IN SCHEMA "
                        + quoteIdent(schema)
                        + " GRANT SELECT ON SEQUENCES TO "
                        + quoteIdent(readRole));
        stmt.execute("ALTER ROLE " + quoteIdent(readRole) + " SET search_path TO " + quoteIdent(schema));
    }

    private void ensureRole(
            Statement stmt, String role, String password, Instant validUntil, boolean inherit)
            throws SQLException {
        boolean exists;
        try (ResultSet rs = stmt.executeQuery(
                "SELECT 1 FROM pg_roles WHERE rolname = " + quoteLiteral(role))) {
            exists = rs.next();
        }
        String inheritClause = inherit ? " INHERIT" : " NOINHERIT";
        String validClause =
                validUntil == null
                        ? " VALID UNTIL 'infinity'"
                        : " VALID UNTIL " + quoteLiteral(validUntil.toString());
        // PG16+ (and prod shared Postgres): CREATEROLE without SUPERUSER cannot set
        // SUPERUSER/REPLICATION/BYPASSRLS attrs — even NOSUPERUSER. Defaults are safe.
        if (exists) {
            stmt.execute("ALTER ROLE " + quoteIdent(role) + " WITH LOGIN PASSWORD " + quoteLiteral(password)
                    + inheritClause + validClause);
        } else {
            stmt.execute("CREATE ROLE " + quoteIdent(role) + " WITH LOGIN PASSWORD " + quoteLiteral(password)
                    + inheritClause + validClause);
        }
    }

    private void ensureSchema(Statement stmt, String schema, String role) throws SQLException {
        // PG15+: CREATE SCHEMA AUTHORIZATION / OWNER TO requires SET ROLE to the owner.
        // CREATEROLE yields ADMIN OPTION on created roles, not membership — grant explicitly.
        stmt.execute("GRANT " + quoteIdent(role) + " TO CURRENT_USER");
        stmt.execute("CREATE SCHEMA IF NOT EXISTS " + quoteIdent(schema) + " AUTHORIZATION " + quoteIdent(role));
        stmt.execute("GRANT USAGE, CREATE ON SCHEMA " + quoteIdent(schema) + " TO " + quoteIdent(role));
        stmt.execute("GRANT ALL ON ALL TABLES IN SCHEMA " + quoteIdent(schema) + " TO " + quoteIdent(role));
        stmt.execute("GRANT ALL ON ALL SEQUENCES IN SCHEMA " + quoteIdent(schema) + " TO " + quoteIdent(role));
        stmt.execute(
                "ALTER DEFAULT PRIVILEGES IN SCHEMA "
                        + quoteIdent(schema)
                        + " GRANT ALL ON TABLES TO "
                        + quoteIdent(role));
        stmt.execute(
                "ALTER DEFAULT PRIVILEGES IN SCHEMA "
                        + quoteIdent(schema)
                        + " GRANT ALL ON SEQUENCES TO "
                        + quoteIdent(role));
    }

    private void grantConnect(Statement stmt, String dbName, String role) throws SQLException {
        stmt.execute("GRANT CONNECT ON DATABASE " + quoteIdent(dbName) + " TO " + quoteIdent(role));
    }

    private static void requireRoleExists(Statement stmt, String role, String message) throws SQLException {
        try (ResultSet rs = stmt.executeQuery(
                "SELECT 1 FROM pg_roles WHERE rolname = " + quoteLiteral(role))) {
            if (!rs.next()) {
                throw new DomainException(message);
            }
        }
    }

    private static void requireSchemaExists(Statement stmt, String schema) throws SQLException {
        try (ResultSet rs = stmt.executeQuery(
                "SELECT 1 FROM information_schema.schemata WHERE schema_name = " + quoteLiteral(schema))) {
            if (!rs.next()) {
                throw new DomainException("Project schema not provisioned: " + schema);
            }
        }
    }

    private Connection openAdmin(String jdbcUrl) throws SQLException {
        AtlasProperties.AppDatabase cfg = properties.getAppDatabase();
        Properties props = new Properties();
        props.setProperty("user", cfg.getUsername().trim());
        props.setProperty("password", cfg.getPassword());
        return DriverManager.getConnection(jdbcUrl, props);
    }

    static String buildAppConnectionUrl(String role, String password, String jdbcUrl, String schema) {
        String hostPort = parseHostPort(jdbcUrl);
        String dbName = parseDatabaseName(jdbcUrl);
        String encodedUser = URLEncoder.encode(role, StandardCharsets.UTF_8);
        String encodedPass = URLEncoder.encode(password, StandardCharsets.UTF_8);
        String encodedSchema = URLEncoder.encode(schema, StandardCharsets.UTF_8);
        return "postgresql://"
                + encodedUser
                + ":"
                + encodedPass
                + "@"
                + hostPort
                + "/"
                + dbName
                + "?currentSchema="
                + encodedSchema;
    }

    static String parseDatabaseName(String jdbcUrl) {
        Matcher m = JDBC_PG.matcher(jdbcUrl.trim());
        if (!m.matches()) {
            throw new DomainException("Unsupported ATLAS_APP_DB_URL (expected jdbc:postgresql://host/db)");
        }
        String db = m.group(3);
        if (db == null || db.isBlank()) {
            throw new DomainException("ATLAS_APP_DB_URL must include a database name (not control-plane atlas)");
        }
        int q = db.indexOf('?');
        return q >= 0 ? db.substring(0, q) : db;
    }

    static String parseHostPort(String jdbcUrl) {
        Matcher m = JDBC_PG.matcher(jdbcUrl.trim());
        if (!m.matches()) {
            throw new DomainException("Unsupported ATLAS_APP_DB_URL (expected jdbc:postgresql://host/db)");
        }
        return m.group(1);
    }

    private String requireJdbcUrl() {
        String url = blankToNull(properties.getAppDatabase().getJdbcUrl());
        if (url == null) {
            throw new DomainException("ATLAS_APP_DB_URL is required");
        }
        ProjectDatabaseNames.rejectControlPlaneDatabase(parseDatabaseName(url));
        return url;
    }

    private static String requireIdent(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainException(field + " is required");
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        if (!trimmed.matches("[a-z_][a-z0-9_]*")) {
            throw new DomainException("Invalid Postgres identifier for " + field + ": " + value);
        }
        return trimmed;
    }

    static String quoteIdent(String ident) {
        return "\"" + ident.replace("\"", "\"\"") + "\"";
    }

    static String quoteLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
