package com.atlas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class AtlasIntegrationTest {

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

    @Test
    void loginAndCreateApplicationAndProjectFlow() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"test-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        String token = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"billing",
                                  "description":"Billing app",
                                  "repositoryUrl":"https://git.example/billing.git",
                                  "branch":"main",
                                  "composePath":"./docker-compose.yml",
                                  "domain":"billing.local"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("billing"))
                .andExpect(jsonPath("$.status").value("REGISTERED"))
                .andExpect(header().string("Deprecation", "true"));

        mockMvc.perform(get("/api/v1/applications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[?(@.name=='billing')]").isNotEmpty());

        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[?(@.name=='billing')].slug").value(org.hamcrest.Matchers.hasItem("billing")));

        mockMvc.perform(get("/api/v1/services")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[?(@.name=='default')]").isNotEmpty());

        mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void authentikSsoProvisionsUserAndIssuesJwt() throws Exception {
        MvcResult sso = mockMvc.perform(get("/api/v1/auth/sso")
                        .header("X-authentik-username", "sso-user")
                        .header("X-authentik-groups", "Atlas Admins")
                        .header("X-authentik-email", "sso@example.com")
                        .header("X-authentik-name", "SSO User")
                        .header("X-authentik-uid", "uid-sso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        String token = com.jayway.jsonpath.JsonPath.read(sso.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("sso-user"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void authentikSsoWithoutHeadersReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/sso")).andExpect(status().isUnauthorized());
    }

    @Test
    void authentikSsoMapsOperatorWithoutAdminGroup() throws Exception {
        MvcResult sso = mockMvc.perform(get("/api/v1/auth/sso")
                        .header("X-authentik-username", "ops-user")
                        .header("X-authentik-groups", "viewers|operators"))
                .andExpect(status().isOk())
                .andReturn();

        String token = com.jayway.jsonpath.JsonPath.read(sso.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ops-user"))
                .andExpect(jsonPath("$.role").value("OPERATOR"));
    }

    @Test
    void hostContainersAndObservabilitySettingsSmoke() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"test-password"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String token = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

        MvcResult host = mockMvc.perform(post("/api/v1/hosts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostname":"edge-runtime",
                                  "ip":"127.0.0.1",
                                  "operatingSystem":"linux",
                                  "dockerVersion":"",
                                  "online":true,
                                  "connectionType":"LOCAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String hostId = com.jayway.jsonpath.JsonPath.read(host.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/hosts/" + hostId + "/containers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/v1/settings/observability")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").exists())
                .andExpect(jsonPath("$.grafanaBaseUrl").exists());
    }

    @Test
    void pipelineCreateAndListRunsSmoke() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"test-password"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String token = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

        MvcResult project = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"pipeline-demo",
                                  "description":"Pipeline smoke",
                                  "repositoryUrl":"https://git.example/demo.git",
                                  "branch":"main",
                                  "composePath":"./docker-compose.yml",
                                  "domain":"demo.local"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String projectId = com.jayway.jsonpath.JsonPath.read(project.getResponse().getContentAsString(), "$.id");

        MvcResult services = mockMvc.perform(get("/api/v1/projects/" + projectId + "/services")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").isNotEmpty())
                .andReturn();

        String serviceId =
                com.jayway.jsonpath.JsonPath.read(services.getResponse().getContentAsString(), "$.content[0].id");

        MvcResult host = mockMvc.perform(post("/api/v1/hosts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostname":"pipeline-host",
                                  "ip":"10.0.0.9",
                                  "operatingSystem":"linux",
                                  "dockerVersion":"",
                                  "online":true,
                                  "connectionType":"LOCAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String hostId = com.jayway.jsonpath.JsonPath.read(host.getResponse().getContentAsString(), "$.id");

        MvcResult pipeline = mockMvc.perform(post("/api/v1/pipelines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId":"%s",
                                  "name":"deploy-default",
                                  "serviceId":"%s",
                                  "hostId":"%s"
                                }
                                """.formatted(projectId, serviceId, hostId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("deploy-default"))
                .andExpect(jsonPath("$.webhookToken").isNotEmpty())
                .andReturn();

        String pipelineId = com.jayway.jsonpath.JsonPath.read(pipeline.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/pipelines/" + pipelineId + "/runs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(post("/api/v1/pipelines/" + pipelineId + "/runs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.deploymentId").isNotEmpty())
                .andExpect(jsonPath("$.jobId").isNotEmpty());

        String webhookToken =
                com.jayway.jsonpath.JsonPath.read(pipeline.getResponse().getContentAsString(), "$.webhookToken");

        mockMvc.perform(post("/api/v1/webhooks/git/" + webhookToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ref\":\"refs/heads/main\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.triggeredBy").value("webhook"))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.deploymentId").isNotEmpty());

        mockMvc.perform(post("/api/v1/webhooks/git/atk_does_not_exist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());

        MvcResult rotated = mockMvc.perform(post("/api/v1/pipelines/" + pipelineId + "/webhook-token/rotate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.webhookToken").isNotEmpty())
                .andReturn();
        String newToken =
                com.jayway.jsonpath.JsonPath.read(rotated.getResponse().getContentAsString(), "$.webhookToken");
        org.junit.jupiter.api.Assertions.assertNotEquals(webhookToken, newToken);

        mockMvc.perform(post("/api/v1/webhooks/git/" + webhookToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/audit")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void operatorWithoutMembershipIsForbiddenOnForeignProject() throws Exception {
        MvcResult adminLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"test-password"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String adminToken =
                com.jayway.jsonpath.JsonPath.read(adminLogin.getResponse().getContentAsString(), "$.accessToken");

        MvcResult project = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"rbac-demo",
                                  "description":"ACL",
                                  "repositoryUrl":"https://git.example/rbac.git",
                                  "branch":"main",
                                  "composePath":"./docker-compose.yml"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String projectId = com.jayway.jsonpath.JsonPath.read(project.getResponse().getContentAsString(), "$.id");

        MvcResult sso = mockMvc.perform(get("/api/v1/auth/sso")
                        .header("X-authentik-username", "ops-no-member")
                        .header("X-authentik-groups", "operators"))
                .andExpect(status().isOk())
                .andReturn();
        String opsToken = com.jayway.jsonpath.JsonPath.read(sso.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(get("/api/v1/projects/" + projectId).header("Authorization", "Bearer " + opsToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/projects").header("Authorization", "Bearer " + opsToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void adminRetentionPurgeSmoke() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"test-password"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(post("/api/v1/admin/purge").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedJobs").isNumber())
                .andExpect(jsonPath("$.deletedPipelineRuns").isNumber())
                .andExpect(jsonPath("$.ran").value(true));

        MvcResult sso = mockMvc.perform(get("/api/v1/auth/sso")
                        .header("X-authentik-username", "ops-purge")
                        .header("X-authentik-groups", "operators"))
                .andExpect(status().isOk())
                .andReturn();
        String opsToken = com.jayway.jsonpath.JsonPath.read(sso.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(post("/api/v1/admin/purge").header("Authorization", "Bearer " + opsToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminDatabaseBackupEnqueueSmoke() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"test-password"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(post("/api/v1/admin/backup").header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.type").value("BACKUP_DATABASE"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        MvcResult sso = mockMvc.perform(get("/api/v1/auth/sso")
                        .header("X-authentik-username", "ops-backup")
                        .header("X-authentik-groups", "operators"))
                .andExpect(status().isOk())
                .andReturn();
        String opsToken = com.jayway.jsonpath.JsonPath.read(sso.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(post("/api/v1/admin/backup").header("Authorization", "Bearer " + opsToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void domainCrudVerifyAndTraefikMetadataSmoke() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"test-password"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

        MvcResult project = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"domain-demo",
                                  "description":"Domains smoke",
                                  "repositoryUrl":"https://git.example/domain.git",
                                  "branch":"main",
                                  "composePath":"./docker-compose.yml",
                                  "domain":"legacy.local"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String projectId = com.jayway.jsonpath.JsonPath.read(project.getResponse().getContentAsString(), "$.id");

        MvcResult services = mockMvc.perform(get("/api/v1/projects/" + projectId + "/services")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String serviceId =
                com.jayway.jsonpath.JsonPath.read(services.getResponse().getContentAsString(), "$.content[0].id");

        MvcResult created = mockMvc.perform(post("/api/v1/projects/" + projectId + "/domains")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hostname":"app.domain-demo.local","serviceId":"%s"}
                                """.formatted(serviceId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hostname").value("app.domain-demo.local"))
                .andExpect(jsonPath("$.status").value("PENDING_DNS"))
                .andExpect(jsonPath("$.verificationToken").isNotEmpty())
                .andExpect(jsonPath("$.dnsTxtName").value("_atlas-challenge.app.domain-demo.local"))
                .andReturn();
        String domainId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/domains")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(domainId));

        mockMvc.perform(post("/api/v1/domains/" + domainId + "/verify")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.certificateIssuer").value("letsencrypt-stub"))
                .andExpect(jsonPath("$.certificateSans").value("app.domain-demo.local"))
                .andExpect(jsonPath("$.verifiedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/domains/" + domainId + "/traefik")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rule").value("Host(`app.domain-demo.local`)"))
                .andExpect(jsonPath("$.labels['traefik.enable']").value("true"))
                .andExpect(jsonPath("$.certResolver").value("letsencrypt"));

        mockMvc.perform(get("/api/v1/domains/" + domainId + "/tunnel-ingress")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostname").value("app.domain-demo.local"))
                .andExpect(jsonPath("$.type").value("HTTPS"))
                .andExpect(jsonPath("$.originUrl").value("traefik:443"))
                .andExpect(jsonPath("$.copyBlock").isNotEmpty());

        mockMvc.perform(post("/api/v1/domains/" + domainId + "/tunnel-ingress/ensure")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("SKIPPED"));

        mockMvc.perform(get("/api/v1/domains/" + domainId + "/dns-cname")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostname").value("app.domain-demo.local"))
                .andExpect(jsonPath("$.copyBlock").isNotEmpty())
                .andExpect(jsonPath("$.proxied").value(true));

        mockMvc.perform(post("/api/v1/domains/" + domainId + "/dns-cname/ensure")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("SKIPPED"));

        mockMvc.perform(get("/api/v1/traefik/routes/" + domainId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routerName").isNotEmpty());

        mockMvc.perform(put("/api/v1/domains/" + domainId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hostname":"www.domain-demo.local","serviceId":"%s"}
                                """.formatted(serviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostname").value("www.domain-demo.local"));

        mockMvc.perform(delete("/api/v1/domains/" + domainId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/domains/" + domainId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void alertRuleAndNotificationChannelCrudSmoke() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"test-password"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

        MvcResult channel = mockMvc.perform(post("/api/v1/notification-channels")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"ops-hook","type":"WEBHOOK","target":"stub://local"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("ops-hook"))
                .andExpect(jsonPath("$.type").value("WEBHOOK"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andReturn();
        String channelId = com.jayway.jsonpath.JsonPath.read(channel.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/notification-channels").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(channelId));

        MvcResult rule = mockMvc.perform(post("/api/v1/alerts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Deploy failed","eventType":"DEPLOY_FAILED","channelId":"%s"}
                                """.formatted(channelId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventType").value("DEPLOY_FAILED"))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.channelId").value(channelId))
                .andReturn();
        String ruleId = com.jayway.jsonpath.JsonPath.read(rule.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/alerts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ruleId));

        mockMvc.perform(post("/api/v1/alerts/" + ruleId + "/silence")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SILENCED"));

        mockMvc.perform(delete("/api/v1/alerts/" + ruleId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/notification-channels/" + channelId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/alerts/" + ruleId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void projectMembershipRolesEnforceViewerDeveloperOperatorMatrix() throws Exception {
        MvcResult adminLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"test-password"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String adminToken =
                com.jayway.jsonpath.JsonPath.read(adminLogin.getResponse().getContentAsString(), "$.accessToken");

        MvcResult project = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"rbac-roles",
                                  "description":"role matrix",
                                  "repositoryUrl":"https://git.example/roles.git",
                                  "branch":"main",
                                  "composePath":"./docker-compose.yml"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String projectId = com.jayway.jsonpath.JsonPath.read(project.getResponse().getContentAsString(), "$.id");

        MvcResult viewerSso = mockMvc.perform(get("/api/v1/auth/sso")
                        .header("X-authentik-username", "role-viewer")
                        .header("X-authentik-groups", "operators"))
                .andExpect(status().isOk())
                .andReturn();
        String viewerToken =
                com.jayway.jsonpath.JsonPath.read(viewerSso.getResponse().getContentAsString(), "$.accessToken");
        MvcResult viewerMe = mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andReturn();
        String viewerUserId =
                com.jayway.jsonpath.JsonPath.read(viewerMe.getResponse().getContentAsString(), "$.id");

        MvcResult developerSso = mockMvc.perform(get("/api/v1/auth/sso")
                        .header("X-authentik-username", "role-developer")
                        .header("X-authentik-groups", "operators"))
                .andExpect(status().isOk())
                .andReturn();
        String developerToken =
                com.jayway.jsonpath.JsonPath.read(developerSso.getResponse().getContentAsString(), "$.accessToken");
        MvcResult developerMe = mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + developerToken))
                .andExpect(status().isOk())
                .andReturn();
        String developerUserId =
                com.jayway.jsonpath.JsonPath.read(developerMe.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/memberships")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","role":"VIEWER"}
                                """.formatted(viewerUserId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("VIEWER"));

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/memberships")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","role":"DEVELOPER"}
                                """.formatted(developerUserId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("DEVELOPER"));

        mockMvc.perform(get("/api/v1/projects/" + projectId).header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("rbac-roles"));

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/services")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"viewer-svc",
                                  "repositoryUrl":"https://git.example/viewer.git",
                                  "branch":"main",
                                  "composePath":"./docker-compose.yml"
                                }
                                """))
                .andExpect(status().isForbidden());

        MvcResult createdService = mockMvc.perform(post("/api/v1/projects/" + projectId + "/services")
                        .header("Authorization", "Bearer " + developerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"dev-svc",
                                  "repositoryUrl":"https://git.example/dev.git",
                                  "branch":"main",
                                  "composePath":"./docker-compose.yml"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("dev-svc"))
                .andReturn();
        String serviceId =
                com.jayway.jsonpath.JsonPath.read(createdService.getResponse().getContentAsString(), "$.id");

        MvcResult host = mockMvc.perform(post("/api/v1/hosts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostname":"rbac-host",
                                  "ip":"10.0.0.77",
                                  "operatingSystem":"linux",
                                  "dockerVersion":"",
                                  "online":true,
                                  "connectionType":"LOCAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String hostId = com.jayway.jsonpath.JsonPath.read(host.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/services/" + serviceId + "/deploy")
                        .header("Authorization", "Bearer " + developerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hostId":"%s"}
                                """.formatted(hostId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/projects/" + projectId).header("Authorization", "Bearer " + developerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void autopilotDeployWithoutHostIdSeedsLocalAndCreatesPublicDomain() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"test-password"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

        MvcResult project = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"autopilot-app",
                                  "description":"Autopilot smoke",
                                  "repositoryUrl":"https://git.example/autopilot.git",
                                  "branch":"main",
                                  "composePath":"./docker-compose.yml"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String projectId = com.jayway.jsonpath.JsonPath.read(project.getResponse().getContentAsString(), "$.id");

        MvcResult services = mockMvc.perform(get("/api/v1/projects/" + projectId + "/services")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].exposure").value("PUBLIC"))
                .andReturn();
        String serviceId =
                com.jayway.jsonpath.JsonPath.read(services.getResponse().getContentAsString(), "$.content[0].id");

        mockMvc.perform(post("/api/v1/services/" + serviceId + "/deploy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exposure":"PUBLIC"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.deploymentId").isNotEmpty())
                .andExpect(jsonPath("$.jobId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/api/v1/hosts")
                        .header("Authorization", "Bearer " + token)
                        .param("hostname", "atlas-local"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].hostname").value("atlas-local"))
                .andExpect(jsonPath("$.content[0].connectionType").value("LOCAL"));

        mockMvc.perform(get("/api/v1/services/" + serviceId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exposure").value("PUBLIC"))
                .andExpect(jsonPath("$.domain").value("default.atlas.local"))
                .andExpect(jsonPath("$.status").value("DEPLOYING"));

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/domains")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hostname").value("default.atlas.local"));

        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"autopilot-internal",
                                  "repositoryUrl":"https://git.example/internal.git",
                                  "branch":"main",
                                  "composePath":"./docker-compose.yml"
                                }
                                """))
                .andExpect(status().isCreated());

        MvcResult internalProject = mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .param("name", "autopilot-internal"))
                .andExpect(status().isOk())
                .andReturn();
        String internalProjectId = com.jayway.jsonpath.JsonPath.read(
                internalProject.getResponse().getContentAsString(), "$.content[0].id");
        MvcResult internalServices = mockMvc.perform(get("/api/v1/projects/" + internalProjectId + "/services")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String internalServiceId = com.jayway.jsonpath.JsonPath.read(
                internalServices.getResponse().getContentAsString(), "$.content[0].id");

        mockMvc.perform(post("/api/v1/services/" + internalServiceId + "/deploy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exposure":"INTERNAL"}
                                """))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/projects/" + internalProjectId + "/domains")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/v1/services/" + internalServiceId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exposure").value("INTERNAL"));
    }

    @Test
    void cronJobCrudSmoke() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"test-password"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

        MvcResult host = mockMvc.perform(post("/api/v1/hosts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hostname":"cron-host",
                                  "ip":"10.0.0.88",
                                  "operatingSystem":"linux",
                                  "dockerVersion":"",
                                  "online":true,
                                  "connectionType":"LOCAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String hostId = com.jayway.jsonpath.JsonPath.read(host.getResponse().getContentAsString(), "$.id");

        MvcResult created = mockMvc.perform(post("/api/v1/cron-jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"sync-cron-host",
                                  "cronExpression":"0 */15 * * * *",
                                  "targetType":"SYNC_HOST",
                                  "targetId":"%s"
                                }
                                """.formatted(hostId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("sync-cron-host"))
                .andExpect(jsonPath("$.targetType").value("SYNC_HOST"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andReturn();
        String cronId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/cron-jobs").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(cronId));

        mockMvc.perform(put("/api/v1/cron-jobs/" + cronId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"sync-cron-host",
                                  "cronExpression":"0 */30 * * * *",
                                  "targetType":"SYNC_HOST",
                                  "targetId":"%s",
                                  "enabled":false
                                }
                                """.formatted(hostId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(delete("/api/v1/cron-jobs/" + cronId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/cron-jobs/" + cronId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

}
