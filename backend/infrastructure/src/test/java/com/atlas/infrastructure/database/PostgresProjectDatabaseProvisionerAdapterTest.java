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
                        "app_demo", "app_demo_migrator", "secret")));
    }

    @Test
    void provisionsSchemaAndRole() throws Exception {
        assertTrue(adapter.isConfigured());
        var result = adapter.provision(new ProjectDatabaseProvisionerPort.ProvisionRequest(
                "app_demo", "app_demo_migrator", "s3cret-pass"));

        assertEquals("app_demo", result.schema());
        assertEquals("apps", result.databaseName());
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

        // Rotate password (idempotent)
        var rotated = adapter.provision(new ProjectDatabaseProvisionerPort.ProvisionRequest(
                "app_demo", "app_demo_migrator", "new-pass-2"));
        assertEquals("app_demo", rotated.schema());
        try (Connection c = DriverManager.getConnection(appsUrl, "app_demo_migrator", "new-pass-2")) {
            assertFalse(c.isClosed());
        }
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
