package com.atlas.infrastructure.database;

import com.atlas.application.port.out.ProjectDatabaseProvisionerPort;
import com.atlas.domain.database.ProjectDatabaseNames;
import com.atlas.domain.shared.DomainException;
import com.atlas.infrastructure.config.AtlasProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Creates schema + migrator role on the shared <strong>apps</strong> Postgres database (ADR-0015).
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
        String password = request.password();
        if (password == null || password.isBlank()) {
            throw new DomainException("password is required");
        }

        AtlasProperties.AppDatabase cfg = properties.getAppDatabase();
        Properties props = new Properties();
        props.setProperty("user", cfg.getUsername().trim());
        props.setProperty("password", cfg.getPassword());

        try (Connection connection = DriverManager.getConnection(jdbcUrl, props);
                Statement stmt = connection.createStatement()) {
            connection.setAutoCommit(true);
            ensureRole(stmt, role, password);
            ensureSchema(stmt, schema, role);
            grantConnect(stmt, dbName, role);
            stmt.execute("ALTER ROLE " + quoteIdent(role) + " SET search_path TO " + quoteIdent(schema));
        } catch (SQLException e) {
            throw new DomainException("Postgres provision failed: " + e.getMessage());
        }

        String connectionUrl = buildAppConnectionUrl(role, password, jdbcUrl, schema);
        return new ProvisionResult(schema, role, dbName, connectionUrl);
    }

    private void ensureRole(Statement stmt, String role, String password) throws SQLException {
        boolean exists;
        try (ResultSet rs = stmt.executeQuery(
                "SELECT 1 FROM pg_roles WHERE rolname = " + quoteLiteral(role))) {
            exists = rs.next();
        }
        if (exists) {
            stmt.execute("ALTER ROLE " + quoteIdent(role) + " WITH LOGIN PASSWORD " + quoteLiteral(password)
                    + " NOSUPERUSER NOCREATEDB NOCREATEROLE");
        } else {
            stmt.execute("CREATE ROLE " + quoteIdent(role) + " WITH LOGIN PASSWORD " + quoteLiteral(password)
                    + " NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT");
        }
    }

    private void ensureSchema(Statement stmt, String schema, String role) throws SQLException {
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
        // strip query if somehow captured
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
