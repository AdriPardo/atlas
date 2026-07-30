package com.atlas.application.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.ProjectDatabaseProvisionerPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ProjectSecretBindingRepositoryPort;
import com.atlas.application.port.out.SecretRepositoryPort;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.project.Project;
import com.atlas.domain.shared.DomainException;
import com.atlas.domain.user.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IssueProjectDatabaseCredentialsUseCaseTest {

    @Mock
    private ProjectRepositoryPort projectRepository;

    @Mock
    private ProjectDatabaseProvisionerPort provisioner;

    @Mock
    private SecretRepositoryPort secretRepository;

    @Mock
    private ProjectSecretBindingRepositoryPort bindingRepository;

    @Mock
    private ProjectAuthorizationService authorizationService;

    @Mock
    private RecordAuditUseCase recordAudit;

    private IssueProjectDatabaseCredentialsUseCase useCase;
    private Project project;
    private final Instant fixed = Instant.parse("2026-07-30T10:00:00Z");

    @BeforeEach
    void setUp() {
        useCase = new IssueProjectDatabaseCredentialsUseCase(
                projectRepository,
                provisioner,
                secretRepository,
                bindingRepository,
                authorizationService,
                recordAudit,
                Clock.fixed(fixed, ZoneOffset.UTC));
        project = Project.create("Reelpath Demo", "demo");
    }

    @Test
    void issuesReadCredential() {
        doNothing().when(authorizationService).require(project.getId(), ProjectPermission.WRITE);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(secretRepository.existsByProjectIdAndName(project.getId(), "db.url")).thenReturn(true);
        when(provisioner.isConfigured()).thenReturn(true);
        when(provisioner.issueCredential(any()))
                .thenAnswer(inv -> {
                    var req = inv.getArgument(0, ProjectDatabaseProvisionerPort.CredentialRequest.class);
                    return new ProjectDatabaseProvisionerPort.CredentialResult(
                            req.temporaryRole(),
                            req.profile(),
                            "postgresql://" + req.temporaryRole() + ":x@h/apps?currentSchema=" + req.schema(),
                            req.validUntil());
                });

        var issued = useCase.issue(project.getId(), null, null);

        assertEquals("db.read", issued.profile());
        assertEquals(60, issued.ttlMinutes());
        assertEquals(fixed.plusSeconds(3600), issued.expiresAt());
        assertTrue(issued.role().startsWith("app_reelpath_demo_t_"));
        verify(recordAudit).execute(eq("PROJECT_DB_CREDENTIAL_ISSUE"), eq("PROJECT"), eq(project.getId()), any());
    }

    @Test
    void migrateRequiresDeploy() {
        doNothing().when(authorizationService).require(project.getId(), ProjectPermission.DEPLOY);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(secretRepository.existsByProjectIdAndName(project.getId(), "db.url")).thenReturn(true);
        when(provisioner.isConfigured()).thenReturn(true);
        when(provisioner.issueCredential(any()))
                .thenReturn(new ProjectDatabaseProvisionerPort.CredentialResult(
                        "app_reelpath_demo_t_deadbeef",
                        "db.migrate",
                        "postgresql://x",
                        fixed.plusSeconds(1800)));

        var issued = useCase.issue(project.getId(), "db.migrate", 30);
        assertEquals("db.migrate", issued.profile());
        assertEquals(30, issued.ttlMinutes());
        verify(authorizationService).require(project.getId(), ProjectPermission.DEPLOY);
    }

    @Test
    void adminAllowedForOperator() {
        doNothing().when(authorizationService).require(project.getId(), ProjectPermission.DEPLOY);
        when(authorizationService.requireActor())
                .thenReturn(new CurrentUserPort.Actor(UUID.randomUUID(), "ops", Role.OPERATOR));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(secretRepository.existsByProjectIdAndName(project.getId(), "db.url")).thenReturn(true);
        when(provisioner.isConfigured()).thenReturn(true);
        when(provisioner.issueCredential(any()))
                .thenReturn(new ProjectDatabaseProvisionerPort.CredentialResult(
                        "app_reelpath_demo_t_admin001",
                        "db.admin",
                        "postgresql://x",
                        fixed.plusSeconds(900)));

        var issued = useCase.issue(project.getId(), "db.admin", 15);
        assertEquals("db.admin", issued.profile());
    }

    @Test
    void requiresProvisioned() {
        doNothing().when(authorizationService).require(project.getId(), ProjectPermission.WRITE);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(secretRepository.existsByProjectIdAndName(project.getId(), "db.url")).thenReturn(false);
        when(bindingRepository.existsByProjectIdAndAlias(project.getId(), "db.url")).thenReturn(false);

        assertThrows(DomainException.class, () -> useCase.issue(project.getId(), "db.read", 60));
    }

    @Test
    void revokeValidatesPrefix() {
        doNothing().when(authorizationService).require(project.getId(), ProjectPermission.WRITE);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(provisioner.isConfigured()).thenReturn(true);

        assertThrows(DomainException.class, () -> useCase.revoke(project.getId(), "other_role"));
    }

    @Test
    void revokeDropsOwnRole() {
        doNothing().when(authorizationService).require(project.getId(), ProjectPermission.WRITE);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(provisioner.isConfigured()).thenReturn(true);

        useCase.revoke(project.getId(), "app_reelpath_demo_t_abcd1234");
        verify(provisioner).revokeCredential("app_reelpath_demo_t_abcd1234");
        verify(recordAudit).execute(eq("PROJECT_DB_CREDENTIAL_REVOKE"), eq("PROJECT"), eq(project.getId()), any());
    }

    @Test
    void listMapsResults() {
        doNothing().when(authorizationService).require(project.getId(), ProjectPermission.READ);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(provisioner.isConfigured()).thenReturn(true);
        when(provisioner.listCredentials("app_reelpath_demo_t_"))
                .thenReturn(List.of(new ProjectDatabaseProvisionerPort.CredentialInfo(
                        "app_reelpath_demo_t_abcd1234", fixed.plusSeconds(60), false)));

        var listed = useCase.list(project.getId());
        assertEquals(1, listed.size());
        assertEquals("app_reelpath_demo_t_abcd1234", listed.getFirst().role());
    }
}
