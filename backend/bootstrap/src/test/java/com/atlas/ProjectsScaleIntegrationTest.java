package com.atlas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlas.domain.organization.Organization;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import javax.sql.DataSource;
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

/**
 * v0.9 criterion: synthetic ~5k projects + smoke list/search stay responsive.
 * Seeds via JDBC batch (not API) to keep CI time bounded.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ProjectsScaleIntegrationTest {

    private static final int SYNTHETIC_COUNT = 5_000;
    /** Soft budget for list/search over 5k rows (ms). Generous for CI hosts. */
    private static final long LATENCY_BUDGET_MS = 2_000L;

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
    private DataSource dataSource;

    @Test
    void listAndSearchStayUnderBudgetWithFiveThousandProjects() throws Exception {
        seedSyntheticProjects(SYNTHETIC_COUNT);

        try (Connection connection = dataSource.getConnection();
                ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM projects")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong(1)).isGreaterThanOrEqualTo(SYNTHETIC_COUNT);
        }

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"test-password"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

        long listStart = System.nanoTime();
        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(SYNTHETIC_COUNT)))
                .andExpect(jsonPath("$.content.length()").value(20));
        long listMs = (System.nanoTime() - listStart) / 1_000_000L;

        long searchStart = System.nanoTime();
        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .param("name", "perf-2499")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value(org.hamcrest.Matchers.containsString("perf-2499")));
        long searchMs = (System.nanoTime() - searchStart) / 1_000_000L;

        assertThat(listMs)
                .as("GET /projects list latency over %d rows was %d ms", SYNTHETIC_COUNT, listMs)
                .isLessThan(LATENCY_BUDGET_MS);
        assertThat(searchMs)
                .as("GET /projects?name= search latency over %d rows was %d ms", SYNTHETIC_COUNT, searchMs)
                .isLessThan(LATENCY_BUDGET_MS);
    }

    private void seedSyntheticProjects(int count) throws Exception {
        String sql =
                """
                INSERT INTO projects (id, organization_id, name, slug, description, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, '', 'REGISTERED', NOW(), NOW())
                ON CONFLICT DO NOTHING
                """;
        UUID orgId = Organization.DEFAULT_ID;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            for (int i = 0; i < count; i++) {
                String label = String.format("perf-%04d", i);
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, orgId);
                ps.setString(3, "Synthetic " + label);
                ps.setString(4, label);
                ps.addBatch();
                if ((i + 1) % 500 == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
            connection.commit();
        }
    }
}
