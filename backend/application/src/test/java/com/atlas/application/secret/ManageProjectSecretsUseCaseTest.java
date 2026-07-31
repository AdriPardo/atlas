package com.atlas.application.secret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ProjectSecretBindingRepositoryPort;
import com.atlas.application.port.out.SecretCipherPort;
import com.atlas.application.port.out.SecretRepositoryPort;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.project.Project;
import com.atlas.domain.secret.ProjectSecretBinding;
import com.atlas.domain.secret.Secret;
import com.atlas.domain.shared.DomainException;
import com.atlas.domain.shared.ForbiddenException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManageProjectSecretsUseCaseTest {

    @Mock
    private SecretRepositoryPort secretRepository;

    @Mock
    private ProjectSecretBindingRepositoryPort bindingRepository;

    @Mock
    private ProjectRepositoryPort projectRepository;

    @Mock
    private SecretCipherPort secretCipher;

    @Mock
    private ProjectAuthorizationService authorizationService;

    private ManageProjectSecretsUseCase useCase;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        useCase = new ManageProjectSecretsUseCase(
                secretRepository, bindingRepository, projectRepository, secretCipher, authorizationService);
        projectId = UUID.randomUUID();
    }

    @Test
    void createOwnedEncryptsAndSaves() {
        Project project = Project.create("demo", "d");
        projectId = project.getId();
        doNothing().when(authorizationService).require(projectId, ProjectPermission.DEPLOY);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(secretRepository.existsByProjectIdAndName(projectId, "git.token")).thenReturn(false);
        when(bindingRepository.existsByProjectIdAndAlias(projectId, "git.token")).thenReturn(false);
        when(secretCipher.encrypt("pat")).thenReturn("cipher");
        when(secretRepository.save(any(Secret.class))).thenAnswer(inv -> inv.getArgument(0));

        Secret saved = useCase.createOwned(projectId, "git.token", "pat");

        assertEquals(projectId, saved.getProjectId());
        assertEquals("git.token", saved.getName());
        assertEquals("cipher", saved.getCiphertext());
    }

    @Test
    void upsertOwnedCreatesWhenMissing() {
        Project project = Project.create("demo", "d");
        projectId = project.getId();
        doNothing().when(authorizationService).require(projectId, ProjectPermission.DEPLOY);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(bindingRepository.existsByProjectIdAndAlias(projectId, "ai.openai")).thenReturn(false);
        when(secretRepository.findByProjectIdAndName(projectId, "ai.openai")).thenReturn(Optional.empty());
        when(secretCipher.encrypt("sk-test")).thenReturn("cipher");
        when(secretRepository.save(any(Secret.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.upsertOwned(projectId, "ai.openai", "sk-test");

        assertEquals(false, result.updated());
        assertEquals("ai.openai", result.secret().getName());
    }

    @Test
    void upsertOwnedReplacesCiphertext() {
        Project project = Project.create("demo", "d");
        projectId = project.getId();
        Secret existing = Secret.createForProject(projectId, "ai.openai", "old-cipher");
        doNothing().when(authorizationService).require(projectId, ProjectPermission.DEPLOY);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(bindingRepository.existsByProjectIdAndAlias(projectId, "ai.openai")).thenReturn(false);
        when(secretRepository.findByProjectIdAndName(projectId, "ai.openai")).thenReturn(Optional.of(existing));
        when(secretCipher.encrypt("sk-new")).thenReturn("new-cipher");
        when(secretRepository.save(any(Secret.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.upsertOwned(projectId, "ai.openai", "sk-new");

        assertEquals(true, result.updated());
        assertEquals("new-cipher", result.secret().getCiphertext());
        assertEquals(existing.getId(), result.secret().getId());
    }

    @Test
    void linkRejectsNonGlobalSecret() {
        Project project = Project.create("demo", "d");
        projectId = project.getId();
        Secret owned = Secret.createForProject(projectId, "db.pass", "c");
        doNothing().when(authorizationService).require(projectId, ProjectPermission.DEPLOY);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(secretRepository.findById(owned.getId())).thenReturn(Optional.of(owned));

        assertThrows(
                DomainException.class, () -> useCase.linkGlobal(projectId, owned.getId(), "git.token"));
    }

    @Test
    void listCombinesOwnedAndLinked() {
        Project project = Project.create("demo", "d");
        projectId = project.getId();
        Secret owned = Secret.createForProject(projectId, "app.key", "c1");
        Secret global = Secret.createGlobal("org-pat", "c2");
        ProjectSecretBinding binding =
                ProjectSecretBinding.create(projectId, global.getId(), "git.token");

        doNothing().when(authorizationService).require(projectId, ProjectPermission.READ);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(secretRepository.findByProjectId(projectId)).thenReturn(List.of(owned));
        when(bindingRepository.findByProjectId(projectId)).thenReturn(List.of(binding));
        when(secretRepository.findById(global.getId())).thenReturn(Optional.of(global));

        List<ManageProjectSecretsUseCase.ProjectSecretEntry> entries = useCase.list(projectId);

        assertEquals(2, entries.size());
        assertEquals("app.key", entries.get(0).name());
        assertEquals(ManageProjectSecretsUseCase.EntryKind.OWNED, entries.get(0).kind());
        assertEquals("git.token", entries.get(1).name());
        assertEquals(ManageProjectSecretsUseCase.EntryKind.LINKED, entries.get(1).kind());
    }

    @Test
    void createOwnedRequiresDeployPermission() {
        org.mockito.Mockito.doThrow(new ForbiddenException("Insufficient project role for DEPLOY"))
                .when(authorizationService)
                .require(projectId, ProjectPermission.DEPLOY);

        assertThrows(ForbiddenException.class, () -> useCase.createOwned(projectId, "x", "y"));
        verify(authorizationService).require(projectId, ProjectPermission.DEPLOY);
    }
}
