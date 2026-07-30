package com.atlas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ProjectDatabaseProvisionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("atlas")
            .withUsername("atlas")
            .withPassword("atlas");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        ensureAppsDatabase();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("atlas.app-database.jdbc-url", () -> postgres.getJdbcUrl().replace("/atlas", "/apps"));
        registry.add("atlas.app-database.username", () -> "atlas");
        registry.add("atlas.app-database.password", () -> "atlas");
    }

    private static void ensureAppsDatabase() {
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
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create apps database", e);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void provisionCreatesSecretsAndStatusShowsProvisioned() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"test-password"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

        MvcResult created = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Db Slice App",
                                  "description": "provisioner it",
                                  "repositoryUrl": "https://github.com/example/db-slice.git",
                                  "branch": "main"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String projectId =
                com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/database")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provisionerConfigured").value(true))
                .andExpect(jsonPath("$.provisioned").value(false))
                .andExpect(jsonPath("$.schema").value("app_db_slice_app"))
                .andExpect(jsonPath("$.role").value("app_db_slice_app_migrator"));

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/database/provision")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schema").value("app_db_slice_app"))
                .andExpect(jsonPath("$.databaseName").value("apps"))
                .andExpect(jsonPath("$.rotated").value(false))
                .andExpect(jsonPath("$.profile").value("db.migrate"));

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/database")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provisioned").value(true))
                .andExpect(jsonPath("$.schema").value("app_db_slice_app"));

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/secrets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='db.url')]").exists())
                .andExpect(jsonPath("$[?(@.name=='db.schema')]").exists());
    }
}
