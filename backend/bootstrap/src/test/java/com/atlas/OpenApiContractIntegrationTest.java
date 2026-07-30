package com.atlas;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlas.api.config.OpenApiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Publishes / verifies the OpenAPI contract for external clients.
 *
 * <p>Regenerate {@code docs/api/openapi.json}:
 *
 * <pre>
 *   ./gradlew :bootstrap:test --tests com.atlas.OpenApiContractIntegrationTest -Datlas.writeOpenApi=true
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class OpenApiContractIntegrationTest {

    private static final List<String> REQUIRED_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/projects",
            "/api/v1/services",
            "/api/v1/hosts",
            "/api/v1/applications",
            "/api/v1/billing/usage",
            "/api/v1/billing/entitlements",
            "/api/v1/settings/features",
            "/api/v1/audit/export");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("atlas")
            .withUsername("atlas")
            .withPassword("atlas");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void openApiContractIsUsableAndApplicationsDeprecated() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Atlas API"))
                .andExpect(jsonPath("$.info.version").value(OpenApiConfig.API_VERSION))
                .andExpect(jsonPath("$.paths['/api/v1/projects']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/services']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/applications'].get.deprecated").value(true))
                .andExpect(jsonPath("$.paths['/api/v1/applications'].post.deprecated").value(true))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andReturn();

        JsonNode live = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(live.path("info").path("description").asText().contains("/api/v1/applications"));
        assertTrue(live.path("info").path("description").asText().contains(OpenApiConfig.APPLICATIONS_SUNSET));

        for (String path : REQUIRED_PATHS) {
            assertTrue(live.path("paths").has(path), "OpenAPI missing required path: " + path);
        }

        Path published = resolvePublishedOpenApi();
        ObjectMapper pretty = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
        boolean write = Boolean.getBoolean("atlas.writeOpenApi")
                || "true".equalsIgnoreCase(System.getenv("ATLAS_WRITE_OPENAPI"));
        if (write) {
            Files.createDirectories(published.getParent());
            pretty.writer().writeValue(published.toFile(), live);
        }

        assertTrue(Files.isRegularFile(published), "Published OpenAPI missing at " + published.toAbsolutePath());
        JsonNode snapshot = objectMapper.readTree(published.toFile());
        assertFalse(snapshot.path("paths").isMissingNode(), "Published OpenAPI has no paths");
        for (String path : REQUIRED_PATHS) {
            assertTrue(snapshot.path("paths").has(path), "Published OpenAPI missing path: " + path);
        }
        assertTrue(
                snapshot.path("paths").path("/api/v1/applications").path("get").path("deprecated").asBoolean(false),
                "Published OpenAPI must mark GET /applications deprecated");

        // Live must still expose every path present in the committed snapshot (no silent removals).
        Iterator<String> snapshotPaths = snapshot.path("paths").fieldNames();
        while (snapshotPaths.hasNext()) {
            String path = snapshotPaths.next();
            assertTrue(live.path("paths").has(path), "Live OpenAPI dropped published path: " + path);
        }
    }

    private static Path resolvePublishedOpenApi() {
        Path cwd = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        List<Path> candidates = List.of(
                cwd.resolve("docs/api/openapi.json").normalize(),
                cwd.resolve("../docs/api/openapi.json").normalize(),
                cwd.resolve("../../docs/api/openapi.json").normalize());
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        // First publish / write: prefer repo docs/ from bootstrap module cwd.
        return cwd.resolve("../../docs/api/openapi.json").normalize();
    }
}
