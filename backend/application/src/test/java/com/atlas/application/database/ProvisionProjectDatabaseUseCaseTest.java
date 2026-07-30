package com.atlas.application.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProvisionProjectDatabaseUseCaseTest {

    @Mock
    private ProjectRepositoryPort projectRepository;

    @Mock
    private ProjectDatabaseProvisionerPort provisioner;

    @Mock
    private SecretRepositoryPort secretRepository;

    @Mock
    private ProjectSecretBindingRepositoryPort bindingRepository;

    @Mock
    private SecretCipherPort secretCipher;

    @Mock
    private ProjectAuthorizationService authorizationService;

    private ProvisionProjectDatabaseUseCase useCase;
    private Project project;

    @BeforeEach
    void setUp() {
        useCase = new ProvisionProjectDatabaseUseCase(
                projectRepository,
                provisioner,
                secretRepository,
                bindingRepository,
                secretCipher,
                authorizationService);
        project = Project.create("Reelpath Demo", "demo");
    }

    @Test
    void statusWhenNotConfigured() {
        doNothing().when(authorizationService).require(project.getId(), ProjectPermission.READ);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(provisioner.isConfigured()).thenReturn(false);
        when(bindingRepository.existsByProjectIdAndAlias(project.getId(), ProjectDatabaseNames.DB_URL_SECRET))
                .thenReturn(false);
        when(secretRepository.existsByProjectIdAndName(project.getId(), ProjectDatabaseNames.DB_URL_SECRET))
                .thenReturn(false);
        when(secretRepository.existsByProjectIdAndName(project.getId(), ProjectDatabaseNames.DB_SCHEMA_SECRET))
                .thenReturn(false);

        var status = useCase.status(project.getId());

        assertFalse(status.provisionerConfigured());
        assertFalse(status.provisioned());
        assertEquals("app_reelpath_demo", status.schema());
        assertEquals("app_reelpath_demo_migrator", status.role());
    }

    @Test
    void provisionStoresSecrets() {
        doNothing().when(authorizationService).require(project.getId(), ProjectPermission.DEPLOY);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(provisioner.isConfigured()).thenReturn(true);
        when(bindingRepository.existsByProjectIdAndAlias(any(), any())).thenReturn(false);
        when(secretRepository.existsByProjectIdAndName(project.getId(), ProjectDatabaseNames.DB_URL_SECRET))
                .thenReturn(false);
        when(secretRepository.findByProjectIdAndName(any(), any())).thenReturn(Optional.empty());
        when(secretCipher.encrypt(any())).thenAnswer(inv -> "enc:" + inv.getArgument(0));
        when(secretRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(provisioner.provision(any()))
                .thenReturn(new ProjectDatabaseProvisionerPort.ProvisionResult(
                        "app_reelpath_demo",
                        "app_reelpath_demo_migrator",
                        "app_reelpath_demo_ro",
                        "apps",
                        "postgresql://app_reelpath_demo_migrator:x@postgres:5432/apps?currentSchema=app_reelpath_demo"));

        var outcome = useCase.provision(project.getId());

        assertEquals("app_reelpath_demo", outcome.schema());
        assertFalse(outcome.rotated());
        ArgumentCaptor<Secret> captor = ArgumentCaptor.forClass(Secret.class);
        verify(secretRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(s -> ProjectDatabaseNames.DB_URL_SECRET.equals(s.getName())));
        assertTrue(captor.getAllValues().stream()
                .anyMatch(s -> ProjectDatabaseNames.DB_SCHEMA_SECRET.equals(s.getName())));
    }

    @Test
    void provisionFailsWhenNotConfigured() {
        doNothing().when(authorizationService).require(project.getId(), ProjectPermission.DEPLOY);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(provisioner.isConfigured()).thenReturn(false);

        assertThrows(DomainException.class, () -> useCase.provision(project.getId()));
    }

    @Test
    void provisionRejectsBoundAlias() {
        doNothing().when(authorizationService).require(project.getId(), ProjectPermission.DEPLOY);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(provisioner.isConfigured()).thenReturn(true);
        when(bindingRepository.existsByProjectIdAndAlias(
                        eq(project.getId()), eq(ProjectDatabaseNames.DB_URL_SECRET)))
                .thenReturn(true);

        assertThrows(DomainException.class, () -> useCase.provision(project.getId()));
    }
}
