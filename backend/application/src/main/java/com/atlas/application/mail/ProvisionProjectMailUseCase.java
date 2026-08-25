package com.atlas.application.mail;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ProjectSecretBindingRepositoryPort;
import com.atlas.application.port.out.ProjectSmtpProvisionerPort;
import com.atlas.application.port.out.SecretCipherPort;
import com.atlas.application.port.out.SecretRepositoryPort;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.mail.ProjectMailNames;
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
public class ProvisionProjectMailUseCase {

    private static final SecureRandom RANDOM = new SecureRandom();

    public record MailStatus(
            boolean provisionerConfigured,
            boolean provisioned,
            String from,
            String host,
            Integer port,
            boolean tls,
            String message) {}

    public record ProvisionOutcome(String from, String host, int port, boolean tls, boolean rotated) {}

    private final ProjectRepositoryPort projectRepository;
    private final ProjectSmtpProvisionerPort provisioner;
    private final SecretRepositoryPort secretRepository;
    private final ProjectSecretBindingRepositoryPort bindingRepository;
    private final SecretCipherPort secretCipher;
    private final ProjectAuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public MailStatus status(UUID projectId) {
        authorizationService.require(projectId, ProjectPermission.READ);
        Project project = requireProject(projectId);
        String from = ProjectMailNames.senderAddress(project.getSlug(), provisioner.fromDomain());
        boolean hasHost = secretPresent(projectId, ProjectMailNames.SMTP_HOST_SECRET);
        if (!provisioner.isConfigured()) {
            return new MailStatus(
                    false,
                    hasHost,
                    from,
                    null,
                    null,
                    false,
                    hasHost
                            ? "smtp.* present (manual). Provisioner not configured (ATLAS_APP_SMTP_HOST)."
                            : "Mail provisioner not configured — set ATLAS_APP_SMTP_HOST.");
        }
        return new MailStatus(
                true,
                hasHost,
                from,
                provisioner.host(),
                provisioner.port(),
                provisioner.tls(),
                hasHost ? "Provisioned (smtp.* secrets present)." : "Ready to provision SMTP credentials.");
    }

    @Transactional
    public ProvisionOutcome provision(UUID projectId) {
        authorizationService.require(projectId, ProjectPermission.DEPLOY);
        return provisionInternal(projectId);
    }

    /**
     * Deploy-worker path: create smtp.* secrets if missing (no RBAC — job already authorized).
     * Idempotent when already provisioned.
     */
    @Transactional
    public Optional<ProvisionOutcome> ensureProvisionedForDeploy(UUID projectId) {
        if (!provisioner.isConfigured()) {
            return Optional.empty();
        }
        if (secretPresent(projectId, ProjectMailNames.SMTP_HOST_SECRET)) {
            return Optional.empty();
        }
        return Optional.of(provisionInternal(projectId));
    }

    private ProvisionOutcome provisionInternal(UUID projectId) {
        Project project = requireProject(projectId);
        if (!provisioner.isConfigured()) {
            throw new DomainException(
                    "Project mail provisioner not configured — set ATLAS_APP_SMTP_HOST on the Atlas install");
        }
        for (String secretName : mailSecretNames()) {
            rejectBoundAlias(projectId, secretName);
        }

        String relayPassword = generateToken();
        String apiToken = generateToken();
        boolean rotated = secretRepository.existsByProjectIdAndName(projectId, ProjectMailNames.SMTP_HOST_SECRET);

        ProjectSmtpProvisionerPort.ProvisionResult result =
                provisioner.provision(new ProjectSmtpProvisionerPort.ProvisionRequest(
                        project.getSlug(), relayPassword, apiToken));

        upsertOwned(projectId, ProjectMailNames.SMTP_HOST_SECRET, result.host());
        upsertOwned(projectId, ProjectMailNames.SMTP_PORT_SECRET, String.valueOf(result.port()));
        upsertOwned(projectId, ProjectMailNames.SMTP_USER_SECRET, result.username());
        upsertOwned(projectId, ProjectMailNames.SMTP_PASSWORD_SECRET, result.password());
        upsertOwned(projectId, ProjectMailNames.SMTP_FROM_SECRET, result.from());
        upsertOwned(projectId, ProjectMailNames.SMTP_TLS_SECRET, Boolean.toString(result.tls()));
        upsertOwned(projectId, ProjectMailNames.MAIL_API_TOKEN_SECRET, result.apiToken());

        return new ProvisionOutcome(result.from(), result.host(), result.port(), result.tls(), rotated);
    }

    private static String[] mailSecretNames() {
        return new String[] {
            ProjectMailNames.SMTP_HOST_SECRET,
            ProjectMailNames.SMTP_PORT_SECRET,
            ProjectMailNames.SMTP_USER_SECRET,
            ProjectMailNames.SMTP_PASSWORD_SECRET,
            ProjectMailNames.SMTP_FROM_SECRET,
            ProjectMailNames.SMTP_TLS_SECRET,
            ProjectMailNames.MAIL_API_TOKEN_SECRET
        };
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
                    "Alias '" + alias + "' is linked from an org secret — unlink before provisioning mail");
        }
    }

    private boolean secretPresent(UUID projectId, String name) {
        return bindingRepository.existsByProjectIdAndAlias(projectId, name)
                || secretRepository.existsByProjectIdAndName(projectId, name);
    }

    private Project requireProject(UUID projectId) {
        return projectRepository
                .findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
    }

    static String generateToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
