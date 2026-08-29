package com.atlas.application.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.atlas.domain.mail.ProjectMailNames;
import com.atlas.domain.project.Project;
import com.atlas.domain.shared.DomainException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SendProjectMailUseCaseTest {

    @Mock
    private ProjectRepositoryPort projectRepository;

    @Mock
    private SecretRepositoryPort secretRepository;

    @Mock
    private ResolveSecretValueUseCase resolveSecretValue;

    @Mock
    private MailSenderPort mailSender;

    @Mock
    private ProjectSmtpProvisionerPort provisioner;

    @Mock
    private ProjectMailRateLimitPort rateLimit;

    @Mock
    private BillingMeterPort billingMeter;

    @Mock
    private RecordAuditUseCase recordAudit;

    @Mock
    private ProjectAuthorizationService authorizationService;

    private SendProjectMailUseCase useCase;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        useCase = new SendProjectMailUseCase(
                projectRepository,
                secretRepository,
                resolveSecretValue,
                mailSender,
                provisioner,
                rateLimit,
                billingMeter,
                recordAudit,
                authorizationService);
        projectId = UUID.randomUUID();
    }

    @Test
    void sessionWithWriteSkipsMailApiToken() {
        Project project = Project.create("demo", "d");
        projectId = project.getId();
        doNothing().when(authorizationService).require(projectId, ProjectPermission.WRITE);
        when(authorizationService.can(projectId, ProjectPermission.WRITE)).thenReturn(true);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(secretRepository.existsByProjectIdAndName(projectId, ProjectMailNames.SMTP_HOST_SECRET))
                .thenReturn(true);
        when(provisioner.dailySendLimitPerProject()).thenReturn(50);
        when(rateLimit.tryConsume(projectId, 50)).thenReturn(true);
        when(rateLimit.remainingToday(projectId, 50)).thenReturn(49);
        stubSmtpSecrets();
        when(mailSender.send(any())).thenReturn(new MailSenderPort.SendResult(true, "queued"));

        var result = useCase.execute(
                projectId,
                new SendProjectMailUseCase.SendMailCommand(
                        "user@example.com",
                        Optional.empty(),
                        Optional.empty(),
                        "Hello",
                        "Body",
                        Optional.empty(),
                        Optional.empty()));

        assertEquals(true, result.sent());
        assertEquals(49, result.remainingToday());
        verify(mailSender).send(any());
    }

    @Test
    void tokenOnlyCallerRequiresMailApiToken() {
        Project project = Project.create("demo", "d");
        projectId = project.getId();
        doNothing().when(authorizationService).require(projectId, ProjectPermission.WRITE);
        when(authorizationService.can(projectId, ProjectPermission.WRITE)).thenReturn(false);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(secretRepository.existsByProjectIdAndName(projectId, ProjectMailNames.SMTP_HOST_SECRET))
                .thenReturn(true);
        when(secretRepository.existsByProjectIdAndName(projectId, ProjectMailNames.MAIL_API_TOKEN_SECRET))
                .thenReturn(true);
        when(resolveSecretValue.forProject(projectId, ProjectMailNames.MAIL_API_TOKEN_SECRET))
                .thenReturn(Optional.of("secret-token"));

        DomainException ex = assertThrows(
                DomainException.class,
                () -> useCase.execute(
                        projectId,
                        new SendProjectMailUseCase.SendMailCommand(
                                "user@example.com",
                                Optional.empty(),
                                Optional.empty(),
                                "Hello",
                                "Body",
                                Optional.empty(),
                                Optional.empty())));

        assertEquals(
                "mail.api_token required — pass X-Atlas-Mail-Token header or apiToken in body",
                ex.getMessage());
    }

    @Test
    void tokenOnlyCallerAcceptsValidMailApiToken() {
        Project project = Project.create("demo", "d");
        projectId = project.getId();
        doNothing().when(authorizationService).require(projectId, ProjectPermission.WRITE);
        when(authorizationService.can(projectId, ProjectPermission.WRITE)).thenReturn(false);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(secretRepository.existsByProjectIdAndName(projectId, ProjectMailNames.SMTP_HOST_SECRET))
                .thenReturn(true);
        when(secretRepository.existsByProjectIdAndName(projectId, ProjectMailNames.MAIL_API_TOKEN_SECRET))
                .thenReturn(true);
        when(resolveSecretValue.forProject(projectId, ProjectMailNames.MAIL_API_TOKEN_SECRET))
                .thenReturn(Optional.of("secret-token"));
        when(provisioner.dailySendLimitPerProject()).thenReturn(50);
        when(rateLimit.tryConsume(projectId, 50)).thenReturn(true);
        when(rateLimit.remainingToday(projectId, 50)).thenReturn(49);
        stubSmtpSecrets();
        when(mailSender.send(any())).thenReturn(new MailSenderPort.SendResult(true, "queued"));

        var result = useCase.execute(
                projectId,
                new SendProjectMailUseCase.SendMailCommand(
                        "user@example.com",
                        Optional.empty(),
                        Optional.empty(),
                        "Hello",
                        "Body",
                        Optional.empty(),
                        Optional.of("secret-token")));

        assertEquals(true, result.sent());
        verify(mailSender).send(any());
    }

    private void stubSmtpSecrets() {
        when(resolveSecretValue.forProject(eq(projectId), eq(ProjectMailNames.SMTP_HOST_SECRET)))
                .thenReturn(Optional.of("smtp"));
        when(resolveSecretValue.forProject(eq(projectId), eq(ProjectMailNames.SMTP_PORT_SECRET)))
                .thenReturn(Optional.of("25"));
        when(resolveSecretValue.forProject(eq(projectId), eq(ProjectMailNames.SMTP_USER_SECRET)))
                .thenReturn(Optional.of(""));
        when(resolveSecretValue.forProject(eq(projectId), eq(ProjectMailNames.SMTP_PASSWORD_SECRET)))
                .thenReturn(Optional.of(""));
        when(resolveSecretValue.forProject(eq(projectId), eq(ProjectMailNames.SMTP_FROM_SECRET)))
                .thenReturn(Optional.of("demo@mail.example"));
        when(resolveSecretValue.forProject(eq(projectId), eq(ProjectMailNames.SMTP_TLS_SECRET)))
                .thenReturn(Optional.of("false"));
    }
}
