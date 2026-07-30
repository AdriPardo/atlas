package com.atlas.infrastructure.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlas.application.port.out.ProjectDatabaseProvisionerPort;
import com.atlas.domain.shared.DomainException;
import com.atlas.infrastructure.config.AtlasProperties;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class PostgresProjectDatabaseProvisionerAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("atlas")
            .withUsername("atlas")
            .withPassword("atlas");

    private AtlasProperties properties;
    private PostgresProjectDatabaseProvisionerAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        try (Connection c = DriverManager.getConnection(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                Statement stmt = c.createStatement()) {
            boolean exists;
            try (ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_database WHERE datname = 'apps'")) {
                exists = rs.next();
            }
            if (!exists) {
                stmt.execute("CREATE DATABASE apps");
            }
        }
        properties = new AtlasProperties();
        String appsUrl = postgres.getJdbcUrl().replace("/atlas", "/apps");
        properties.getAppDatabase().setJdbcUrl(appsUrl);
        properties.getAppDatabase().setUsername("atlas");
        properties.getAppDatabase().setPassword("atlas");
        adapter = new PostgresProjectDatabaseProvisionerAdapter(properties);
    }

    @Test
    void refusesControlPlaneDatabase() {
        properties.getAppDatabase().setJdbcUrl(postgres.getJdbcUrl());
        assertThrows(DomainException.class, adapter::databaseName);
        assertThrows(
                DomainException.class,
                () -> adapter.provision(new ProjectDatabaseProvisionerPort.ProvisionRequest(
                        "app_demo", "app_demo_migrator", "app_demo_ro", "secret")));
    }

    @Test
    void provisionsSchemaAndRole() throws Exception {
        assertTrue(adapter.isConfigured());
        var result = adapter.provision(new ProjectDatabaseProvisionerPort.ProvisionRequest(
                "app_demo", "app_demo_migrator", "app_demo_ro", "s3cret-pass"));

        assertEquals("app_demo", result.schema());
        assertEquals("apps", result.databaseName());
        assertEquals("app_demo_ro", result.readRole());
        assertTrue(result.connectionUrl().startsWith("postgresql://app_demo_migrator:"));
        assertTrue(result.connectionUrl().contains("/apps?currentSchema=app_demo"));

        String appsUrl = postgres.getJdbcUrl().replace("/atlas", "/apps");
        try (Connection c = DriverManager.getConnection(appsUrl, "app_demo_migrator", "s3cret-pass");
                Statement stmt = c.createStatement()) {
            stmt.execute("CREATE TABLE app_demo.t (id int)");
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT 1 FROM information_schema.schemata WHERE schema_name = 'app_demo'")) {
                assertTrue(rs.next());
            }
        }

        var rotated = adapter.provision(new ProjectDatabaseProvisionerPort.ProvisionRequest(
                "app_demo", "app_demo_migrator", "app_demo_ro", "new-pass-2"));
        assertEquals("app_demo", rotated.schema());
        try (Connection c = DriverManager.getConnection(appsUrl, "app_demo_migrator", "new-pass-2")) {
            assertFalse(c.isClosed());
        }
    }

    @Test
    void issuesAndRevokesTtlReadCredential() throws Exception {
        adapter.provision(new ProjectDatabaseProvisionerPort.ProvisionRequest(
                "app_ttl", "app_ttl_migrator", "app_ttl_ro", "mig-pass"));
        String appsUrl = postgres.getJdbcUrl().replace("/atlas", "/apps");
        try (Connection c = DriverManager.getConnection(appsUrl, "app_ttl_migrator", "mig-pass");
                Statement stmt = c.createStatement()) {
            stmt.execute("CREATE TABLE app_ttl.items (id int PRIMARY KEY, name text)");
            stmt.execute("INSERT INTO app_ttl.items VALUES (1, 'ok')");
        }

        Instant until = Instant.now().plus(30, ChronoUnit.MINUTES);
        var cred = adapter.issueCredential(new ProjectDatabaseProvisionerPort.CredentialRequest(
                "app_ttl",
                "app_ttl_migrator",
                "app_ttl_ro",
                "app_ttl_t_abcd1234",
                "tmp-pass",
                until,
                "db.read"));
        assertEquals("db.read", cred.profile());
        assertTrue(cred.connectionUrl().contains("app_ttl_t_abcd1234"));

        try (Connection c = DriverManager.getConnection(appsUrl, "app_ttl_t_abcd1234", "tmp-pass");
                Statement stmt = c.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT name FROM app_ttl.items")) {
            assertTrue(rs.next());
            assertEquals("ok", rs.getString(1));
        }

        // read must not write
        assertThrows(Exception.class, () -> {
            try (Connection c = DriverManager.getConnection(appsUrl, "app_ttl_t_abcd1234", "tmp-pass");
                    Statement stmt = c.createStatement()) {
                stmt.execute("INSERT INTO app_ttl.items VALUES (2, 'no')");
            }
        });

        assertEquals(1, adapter.listCredentials("app_ttl_t_").size());
        adapter.revokeCredential("app_ttl_t_abcd1234");
        assertTrue(adapter.listCredentials("app_ttl_t_").isEmpty());
        assertThrows(Exception.class, () -> DriverManager.getConnection(appsUrl, "app_ttl_t_abcd1234", "tmp-pass"));
    }

    @Test
    void parseHelpers() {
        assertEquals(
                "apps",
                PostgresProjectDatabaseProvisionerAdapter.parseDatabaseName(
                        "jdbc:postgresql://postgres:5432/apps"));
        assertEquals(
                "postgres:5432",
                PostgresProjectDatabaseProvisionerAdapter.parseHostPort(
                        "jdbc:postgresql://postgres:5432/apps"));
    }
}
