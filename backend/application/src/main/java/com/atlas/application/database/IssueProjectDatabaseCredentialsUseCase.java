package com.atlas.application.database;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.ProjectDatabaseProvisionerPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ProjectSecretBindingRepositoryPort;
import com.atlas.application.port.out.SecretRepositoryPort;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.database.DatabaseAccessProfile;
import com.atlas.domain.database.ProjectDatabaseNames;
import com.atlas.domain.project.Project;
import com.atlas.domain.shared.DomainException;
import com.atlas.domain.shared.ForbiddenException;
import com.atlas.domain.shared.NotFoundException;
import com.atlas.domain.user.Role;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-0015 option C: issue / list / revoke short-lived DB credentials without rotating {@code db.url}.
 */
@Service
public class IssueProjectDatabaseCredentialsUseCase {

    private static final SecureRandom RANDOM = new SecureRandom();

    public record IssuedCredential(
            String role, String profile, String connectionUrl, Instant expiresAt, int ttlMinutes) {}

    public record ListedCredential(String role, Instant expiresAt, boolean expired) {}

    private final ProjectRepositoryPort projectRepository;
    private final ProjectDatabaseProvisionerPort provisioner;
    private final SecretRepositoryPort secretRepository;
    private final ProjectSecretBindingRepositoryPort bindingRepository;
    private final ProjectAuthorizationService authorizationService;
    private final RecordAuditUseCase recordAudit;
    private final Clock clock;

    @Autowired
    public IssueProjectDatabaseCredentialsUseCase(
            ProjectRepositoryPort projectRepository,
            ProjectDatabaseProvisionerPort provisioner,
            SecretRepositoryPort secretRepository,
            ProjectSecretBindingRepositoryPort bindingRepository,
            ProjectAuthorizationService authorizationService,
            RecordAuditUseCase recordAudit) {
        this(
                projectRepository,
                provisioner,
                secretRepository,
                bindingRepository,
                authorizationService,
                recordAudit,
                Clock.systemUTC());
    }

    IssueProjectDatabaseCredentialsUseCase(
            ProjectRepositoryPort projectRepository,
            ProjectDatabaseProvisionerPort provisioner,
            SecretRepositoryPort secretRepository,
            ProjectSecretBindingRepositoryPort bindingRepository,
            ProjectAuthorizationService authorizationService,
            RecordAuditUseCase recordAudit,
            Clock clock) {
        this.projectRepository = projectRepository;
        this.provisioner = provisioner;
        this.secretRepository = secretRepository;
        this.bindingRepository = bindingRepository;
        this.authorizationService = authorizationService;
        this.recordAudit = recordAudit;
        this.clock = clock;
    }

    @Transactional
    public IssuedCredential issue(UUID projectId, String profileWire, Integer ttlMinutes) {
        DatabaseAccessProfile profile = DatabaseAccessProfile.fromWire(profileWire);
        requirePermissionForProfile(projectId, profile);
        Project project = requireProject(projectId);
        requireProvisioned(projectId);
        if (!provisioner.isConfigured()) {
            throw new DomainException(
                    "Project DB provisioner not configured — set ATLAS_APP_DB_URL (must not be database 'atlas')");
        }

        int ttl = ProjectDatabaseNames.clampTtlMinutes(ttlMinutes);
        Instant expiresAt = clock.instant().plus(ttl, ChronoUnit.MINUTES);
        String suffix = randomSuffix();
        String temporaryRole = ProjectDatabaseNames.temporaryCredentialRole(project.getSlug(), suffix);
        String password = ProvisionProjectDatabaseUseCase.generatePassword();

        ProjectDatabaseProvisionerPort.CredentialResult result =
                provisioner.issueCredential(new ProjectDatabaseProvisionerPort.CredentialRequest(
                        ProjectDatabaseNames.schemaName(project.getSlug()),
                        ProjectDatabaseNames.migratorRole(project.getSlug()),
                        ProjectDatabaseNames.readOnlyRole(project.getSlug()),
                        temporaryRole,
                        password,
                        expiresAt,
                        profile.wire()));

        recordAudit.execute(
                "PROJECT_DB_CREDENTIAL_ISSUE",
                "PROJECT",
                projectId,
                "profile=" + profile.wire() + ",role=" + result.role() + ",ttlMinutes=" + ttl);

        return new IssuedCredential(
                result.role(), result.profile(), result.connectionUrl(), result.expiresAt(), ttl);
    }

    @Transactional(readOnly = true)
    public List<ListedCredential> list(UUID projectId) {
        authorizationService.require(projectId, ProjectPermission.READ);
        Project project = requireProject(projectId);
        if (!provisioner.isConfigured()) {
            return List.of();
        }
        String prefix = ProjectDatabaseNames.temporaryCredentialRolePrefix(project.getSlug());
        return provisioner.listCredentials(prefix).stream()
                .map(c -> new ListedCredential(c.role(), c.expiresAt(), c.expired()))
                .toList();
    }

    @Transactional
    public void revoke(UUID projectId, String role) {
        authorizationService.require(projectId, ProjectPermission.WRITE);
        Project project = requireProject(projectId);
        if (!provisioner.isConfigured()) {
            throw new DomainException("Project DB provisioner not configured");
        }
        String safeRole = role == null ? "" : role.trim().toLowerCase();
        String prefix = ProjectDatabaseNames.temporaryCredentialRolePrefix(project.getSlug());
        if (!safeRole.startsWith(prefix)) {
            throw new DomainException("Role does not belong to this project's TTL credentials");
        }
        provisioner.revokeCredential(safeRole);
        recordAudit.execute(
                "PROJECT_DB_CREDENTIAL_REVOKE", "PROJECT", projectId, "role=" + safeRole);
    }

    private void requirePermissionForProfile(UUID projectId, DatabaseAccessProfile profile) {
        switch (profile) {
            case READ -> authorizationService.require(projectId, ProjectPermission.WRITE);
            case MIGRATE -> authorizationService.require(projectId, ProjectPermission.DEPLOY);
            case ADMIN -> {
                authorizationService.require(projectId, ProjectPermission.DEPLOY);
                CurrentUserPort.Actor actor = authorizationService.requireActor();
                if (!actor.isAdmin() && actor.role() != Role.OPERATOR) {
                    throw new ForbiddenException("db.admin requires global ADMIN or OPERATOR");
                }
            }
        }
    }

    private void requireProvisioned(UUID projectId) {
        boolean hasUrl = bindingRepository.existsByProjectIdAndAlias(projectId, ProjectDatabaseNames.DB_URL_SECRET)
                || secretRepository.existsByProjectIdAndName(projectId, ProjectDatabaseNames.DB_URL_SECRET);
        if (!hasUrl) {
            throw new DomainException("Provision project database first (missing db.url)");
        }
    }

    private Project requireProject(UUID projectId) {
        return projectRepository
                .findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
    }

    private static String randomSuffix() {
        byte[] bytes = new byte[4];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
