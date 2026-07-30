package com.atlas.application.database;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.ProjectDatabaseProvisionerPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ProjectSecretBindingRepositoryPort;
import com.atlas.application.port.out.SecretCipherPort;
import com.atlas.application.port.out.SecretRepositoryPort;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.database.ProjectDatabaseNames;
import com.atlas.domain.project.Project;
import com.atlas.domain.secret.Secret;
import com.atlas.domain.shared.DomainException;
import com.atlas.domain.shared.NotFoundException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProvisionProjectDatabaseUseCase {

    private static final SecureRandom RANDOM = new SecureRandom();

    public record DatabaseStatus(
            boolean provisionerConfigured,
            boolean provisioned,
            String schema,
            String role,
            String databaseName,
            String profile,
            String message) {}

    public record ProvisionOutcome(
            String schema, String role, String databaseName, String profile, boolean rotated) {}

    private final ProjectRepositoryPort projectRepository;
    private final ProjectDatabaseProvisionerPort provisioner;
    private final SecretRepositoryPort secretRepository;
    private final ProjectSecretBindingRepositoryPort bindingRepository;
    private final SecretCipherPort secretCipher;
    private final ProjectAuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public DatabaseStatus status(UUID projectId) {
        authorizationService.require(projectId, ProjectPermission.READ);
        Project project = requireProject(projectId);
        String schema = ProjectDatabaseNames.schemaName(project.getSlug());
        String role = ProjectDatabaseNames.migratorRole(project.getSlug());
        boolean hasUrl = secretPresent(projectId, ProjectDatabaseNames.DB_URL_SECRET);
        Optional<String> schemaSecret = ownedOrBoundName(projectId, ProjectDatabaseNames.DB_SCHEMA_SECRET);
        String displaySchema = schemaSecret.orElse(schema);
        if (!provisioner.isConfigured()) {
            return new DatabaseStatus(
                    false,
                    hasUrl,
                    displaySchema,
                    role,
                    null,
                    ProjectDatabaseNames.DEFAULT_PROFILE,
                    hasUrl
                            ? "db.url present (manual). Provisioner not configured (ATLAS_APP_DB_URL)."
                            : "Provisioner not configured — set ATLAS_APP_DB_URL to a dedicated apps database.");
        }
        return new DatabaseStatus(
                true,
                hasUrl,
                displaySchema,
                role,
                provisioner.databaseName(),
                ProjectDatabaseNames.DEFAULT_PROFILE,
                hasUrl ? "Provisioned (db.url present)." : "Ready to provision schema + migrator role.");
    }

    @Transactional
    public ProvisionOutcome provision(UUID projectId) {
        authorizationService.require(projectId, ProjectPermission.DEPLOY);
        Project project = requireProject(projectId);
        if (!provisioner.isConfigured()) {
            throw new DomainException(
                    "Project DB provisioner not configured — set ATLAS_APP_DB_URL (must not be database 'atlas')");
        }
        rejectBoundAlias(projectId, ProjectDatabaseNames.DB_URL_SECRET);
        rejectBoundAlias(projectId, ProjectDatabaseNames.DB_SCHEMA_SECRET);

        String schema = ProjectDatabaseNames.schemaName(project.getSlug());
        String role = ProjectDatabaseNames.migratorRole(project.getSlug());
        String readRole = ProjectDatabaseNames.readOnlyRole(project.getSlug());
        String password = generatePassword();
        boolean rotated = secretRepository.existsByProjectIdAndName(projectId, ProjectDatabaseNames.DB_URL_SECRET);

        ProjectDatabaseProvisionerPort.ProvisionResult result =
                provisioner.provision(new ProjectDatabaseProvisionerPort.ProvisionRequest(
                        schema, role, readRole, password));

        upsertOwned(projectId, ProjectDatabaseNames.DB_URL_SECRET, result.connectionUrl());
        upsertOwned(projectId, ProjectDatabaseNames.DB_SCHEMA_SECRET, result.schema());

        return new ProvisionOutcome(
                result.schema(),
                result.role(),
                result.databaseName(),
                ProjectDatabaseNames.DEFAULT_PROFILE,
                rotated);
    }

    private void upsertOwned(UUID projectId, String name, String value) {
        String ciphertext = secretCipher.encrypt(value);
        Optional<Secret> existing = secretRepository.findByProjectIdAndName(projectId, name);
        if (existing.isPresent()) {
            secretRepository.save(existing.get().withCiphertext(ciphertext));
        } else {
            secretRepository.save(Secret.createForProject(projectId, name, ciphertext));
        }
    }

    private void rejectBoundAlias(UUID projectId, String alias) {
        if (bindingRepository.existsByProjectIdAndAlias(projectId, alias)) {
            throw new DomainException(
                    "Alias '" + alias + "' is linked from an org secret — unlink before provisioning");
        }
    }

    private boolean secretPresent(UUID projectId, String name) {
        return bindingRepository.existsByProjectIdAndAlias(projectId, name)
                || secretRepository.existsByProjectIdAndName(projectId, name);
    }

    private Optional<String> ownedOrBoundName(UUID projectId, String name) {
        if (secretRepository.existsByProjectIdAndName(projectId, name)) {
            // Value is encrypted; for db.schema we still need the plaintext for UI.
            return secretRepository
                    .findByProjectIdAndName(projectId, name)
                    .map(s -> secretCipher.decrypt(s.getCiphertext()));
        }
        return Optional.empty();
    }

    private Project requireProject(UUID projectId) {
        return projectRepository
                .findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
    }

    static String generatePassword() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
