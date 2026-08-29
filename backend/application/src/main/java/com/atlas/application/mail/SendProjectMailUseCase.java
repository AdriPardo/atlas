package com.atlas.application.mail;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.port.out.BillingMeterPort;
import com.atlas.application.port.out.MailSenderPort;
import com.atlas.application.port.out.ProjectMailRateLimitPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ProjectSmtpProvisionerPort;
import com.atlas.application.port.out.SecretRepositoryPort;
import com.atlas.application.secret.ResolveSecretValueUseCase;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.billing.UsageMeters;
import com.atlas.domain.mail.ProjectMailNames;
import com.atlas.domain.project.Project;
import com.atlas.domain.shared.DomainException;
import com.atlas.domain.shared.NotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SendProjectMailUseCase {

    public record SendMailCommand(
            String to,
            Optional<String> cc,
            Optional<String> bcc,
            String subject,
            String textBody,
            Optional<String> htmlBody,
            Optional<String> apiToken) {}

    public record SendMailResult(boolean sent, String detail, int remainingToday) {}

    private final ProjectRepositoryPort projectRepository;
    private final SecretRepositoryPort secretRepository;
    private final ResolveSecretValueUseCase resolveSecretValue;
    private final MailSenderPort mailSender;
    private final ProjectSmtpProvisionerPort provisioner;
    private final ProjectMailRateLimitPort rateLimit;
    private final BillingMeterPort billingMeter;
    private final RecordAuditUseCase recordAudit;
    private final ProjectAuthorizationService authorizationService;

    @Transactional
    public SendMailResult execute(UUID projectId, SendMailCommand command) {
        authorizationService.require(projectId, ProjectPermission.DEPLOY);
        Project project = requireProject(projectId);
        requireProvisioned(projectId);
        verifyApiTokenIfConfigured(projectId, command.apiToken());

        int dailyLimit = Math.max(1, provisioner.dailySendLimitPerProject());
        if (!rateLimit.tryConsume(projectId, dailyLimit)) {
            throw new DomainException("Daily mail send limit reached for this project (" + dailyLimit + "/day)");
        }

        MailCredentials creds = loadCredentials(projectId);
        MailSenderPort.SendResult result = mailSender.send(new MailSenderPort.SendRequest(
                creds.host(),
                creds.port(),
                creds.tls(),
                creds.auth(),
                creds.username(),
                creds.password(),
                creds.from(),
                List.of(requireEmail(command.to(), "to")),
                command.cc().map(v -> requireEmail(v, "cc")),
                command.bcc().map(v -> requireEmail(v, "bcc")),
                requireText(command.subject(), "subject"),
                requireText(command.textBody(), "textBody"),
                command.htmlBody()));

        if (result.sent()) {
            billingMeter.record(
                    UsageMeters.MAIL_SEND_COUNT,
                    BigDecimal.ONE,
                    Map.of("projectId", projectId.toString(), "projectSlug", project.getSlug()));
            recordAudit.execute(
                    "mail.send",
                    "project",
                    projectId,
                    "{\"to\":\"" + escapeJson(command.to()) + "\",\"subject\":\""
                            + escapeJson(truncate(command.subject(), 120)) + "\"}");
        }

        return new SendMailResult(
                result.sent(), result.detail(), rateLimit.remainingToday(projectId, dailyLimit));
    }

    @Transactional(readOnly = true)
    public int remainingToday(UUID projectId) {
        authorizationService.require(projectId, ProjectPermission.READ);
        int dailyLimit = Math.max(1, provisioner.dailySendLimitPerProject());
        return rateLimit.remainingToday(projectId, dailyLimit);
    }

    private void verifyApiTokenIfConfigured(UUID projectId, Optional<String> provided) {
        // UI test send and other session-authenticated deploy operators skip mail.api_token.
        if (authorizationService.can(projectId, ProjectPermission.DEPLOY)) {
            return;
        }
        if (!secretRepository.existsByProjectIdAndName(projectId, ProjectMailNames.MAIL_API_TOKEN_SECRET)) {
            return;
        }
        String expected = resolveSecretValue
                .forProject(projectId, ProjectMailNames.MAIL_API_TOKEN_SECRET)
                .orElseThrow(() -> new DomainException("mail.api_token secret missing"));
        String token = provided.filter(t -> !t.isBlank()).orElseThrow(() -> new DomainException(
                "mail.api_token required — pass X-Atlas-Mail-Token header or apiToken in body"));
        if (!expected.equals(token)) {
            throw new DomainException("Invalid mail API token");
        }
    }

    private void requireProvisioned(UUID projectId) {
        if (!secretRepository.existsByProjectIdAndName(projectId, ProjectMailNames.SMTP_HOST_SECRET)) {
            throw new DomainException("Project mail not provisioned — provision SMTP credentials first");
        }
    }

    private MailCredentials loadCredentials(UUID projectId) {
        String host = requireSecret(projectId, ProjectMailNames.SMTP_HOST_SECRET);
        String portRaw = requireSecret(projectId, ProjectMailNames.SMTP_PORT_SECRET);
        int port;
        try {
            port = Integer.parseInt(portRaw.trim());
        } catch (NumberFormatException ex) {
            throw new DomainException("Invalid smtp.port secret");
        }
        String username = optionalSecret(projectId, ProjectMailNames.SMTP_USER_SECRET);
        String password = optionalSecret(projectId, ProjectMailNames.SMTP_PASSWORD_SECRET);
        String from = requireSecret(projectId, ProjectMailNames.SMTP_FROM_SECRET);
        boolean tls = Boolean.parseBoolean(optionalSecret(projectId, ProjectMailNames.SMTP_TLS_SECRET));
        boolean auth = username != null && !username.isBlank();
        return new MailCredentials(
                host, port, tls, auth, username == null ? "" : username, password == null ? "" : password, from);
    }

    private String requireSecret(UUID projectId, String name) {
        return resolveSecretValue
                .forProject(projectId, name)
                .filter(v -> !v.isBlank())
                .orElseThrow(() -> new DomainException("Missing project secret: " + name));
    }

    private String optionalSecret(UUID projectId, String name) {
        return resolveSecretValue.forProject(projectId, name).orElse("");
    }

    private Project requireProject(UUID projectId) {
        return projectRepository
                .findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainException(field + " is required");
        }
        return value.trim();
    }

    private static String requireEmail(String value, String field) {
        String trimmed = requireText(value, field);
        if (!trimmed.contains("@") || trimmed.startsWith("@") || trimmed.endsWith("@")) {
            throw new DomainException("Invalid " + field + " email");
        }
        return trimmed;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record MailCredentials(
            String host, int port, boolean tls, boolean auth, String username, String password, String from) {}
}
