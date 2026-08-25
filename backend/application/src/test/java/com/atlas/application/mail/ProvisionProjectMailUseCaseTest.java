package com.atlas.application.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProvisionProjectMailUseCaseTest {

    @Mock
    private ProjectRepositoryPort projectRepository;

    @Mock
    private ProjectSmtpProvisionerPort provisioner;

    @Mock
    private SecretRepositoryPort secretRepository;

    @Mock
    private ProjectSecretBindingRepositoryPort bindingRepository;

    @Mock
    private SecretCipherPort secretCipher;

    @Mock
    private ProjectAuthorizationService authorizationService;

    private ProvisionProjectMailUseCase useCase;
    private Project project;

    @BeforeEach
    void setUp() {
        useCase = new ProvisionProjectMailUseCase(
                projectRepository,
                provisioner,
                secretRepository,
                bindingRepository,
                secretCipher,
                authorizationService);
        project = Project.create("Reelpath Demo", "demo app");
    }

    @Test
    void statusWhenNotConfigured() {
        doNothing().when(authorizationService).require(project.getId(), ProjectPermission.READ);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(provisioner.isConfigured()).thenReturn(false);
        when(provisioner.fromDomain()).thenReturn("mail.atlas.local");
        when(bindingRepository.existsByProjectIdAndAlias(project.getId(), ProjectMailNames.SMTP_HOST_SECRET))
                .thenReturn(false);
        when(secretRepository.existsByProjectIdAndName(project.getId(), ProjectMailNames.SMTP_HOST_SECRET))
                .thenReturn(false);

        var status = useCase.status(project.getId());

        assertFalse(status.provisionerConfigured());
        assertFalse(status.provisioned());
        assertEquals("reelpath_demo@mail.atlas.local", status.from());
    }

    @Test
    void provisionStoresSecrets() {
        doNothing().when(authorizationService).require(project.getId(), ProjectPermission.DEPLOY);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(provisioner.isConfigured()).thenReturn(true);
        when(bindingRepository.existsByProjectIdAndAlias(any(), any())).thenReturn(false);
        when(secretRepository.existsByProjectIdAndName(project.getId(), ProjectMailNames.SMTP_HOST_SECRET))
                .thenReturn(false);
        when(secretRepository.findByProjectIdAndName(any(), any())).thenReturn(Optional.empty());
        when(secretCipher.encrypt(any())).thenAnswer(inv -> "enc:" + inv.getArgument(0));
        when(secretRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(provisioner.provision(any()))
                .thenReturn(new ProjectSmtpProvisionerPort.ProvisionResult(
                        "mailpit", 1025, "mail_reelpath_demo", "secret", "reelpath_demo@mail.atlas.local", false, "api-token"));

        var outcome = useCase.provision(project.getId());

        assertEquals("reelpath_demo@mail.atlas.local", outcome.from());
        assertFalse(outcome.rotated());
        ArgumentCaptor<Secret> captor = ArgumentCaptor.forClass(Secret.class);
        verify(secretRepository, org.mockito.Mockito.atLeast(5)).save(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(s -> ProjectMailNames.SMTP_HOST_SECRET.equals(s.getName())));
        assertTrue(captor.getAllValues().stream()
                .anyMatch(s -> ProjectMailNames.MAIL_API_TOKEN_SECRET.equals(s.getName())));
    }

    @Test
    void provisionFailsWhenNotConfigured() {
        doNothing().when(authorizationService).require(project.getId(), ProjectPermission.DEPLOY);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(provisioner.isConfigured()).thenReturn(false);

        assertThrows(DomainException.class, () -> useCase.provision(project.getId()));
    }
}
