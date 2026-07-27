package com.atlas.application.secret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.atlas.application.port.out.ProjectSecretBindingRepositoryPort;
import com.atlas.application.port.out.SecretCipherPort;
import com.atlas.application.port.out.SecretRepositoryPort;
import com.atlas.domain.secret.ProjectSecretBinding;
import com.atlas.domain.secret.Secret;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResolveSecretValueUseCaseTest {

    @Mock
    private SecretRepositoryPort secretRepository;

    @Mock
    private ProjectSecretBindingRepositoryPort bindingRepository;

    @Mock
    private SecretCipherPort secretCipher;

    private ResolveSecretValueUseCase useCase;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        useCase = new ResolveSecretValueUseCase(secretRepository, bindingRepository, secretCipher);
        projectId = UUID.randomUUID();
    }

    @Test
    void forProjectPrefersBindingAliasOverOwnedAndGlobal() {
        Secret linked = Secret.createGlobal("org-pat", "cipher-linked");
        Secret owned = Secret.createForProject(projectId, "git.token", "cipher-owned");
        Secret global = Secret.createGlobal("git.token", "cipher-global");
        ProjectSecretBinding binding =
                ProjectSecretBinding.create(projectId, linked.getId(), "git.token");

        when(bindingRepository.findByProjectIdAndAlias(projectId, "git.token"))
                .thenReturn(Optional.of(binding));
        when(secretRepository.findById(linked.getId())).thenReturn(Optional.of(linked));
        when(secretCipher.decrypt("cipher-linked")).thenReturn("token-from-binding");

        Optional<String> value = useCase.forProject(projectId, "git.token");

        assertEquals(Optional.of("token-from-binding"), value);
        // owned/global must not be consulted when binding hits
        assertTrue(owned.getName().equals("git.token"));
        assertTrue(global.isGlobal());
    }

    @Test
    void forProjectFallsBackToOwnedThenGlobal() {
        Secret owned = Secret.createForProject(projectId, "git.token", "cipher-owned");
        when(bindingRepository.findByProjectIdAndAlias(projectId, "git.token"))
                .thenReturn(Optional.empty());
        when(secretRepository.findByProjectIdAndName(projectId, "git.token"))
                .thenReturn(Optional.of(owned));
        when(secretCipher.decrypt("cipher-owned")).thenReturn("token-owned");

        assertEquals(Optional.of("token-owned"), useCase.forProject(projectId, "git.token"));
    }

    @Test
    void forProjectFallsBackToGlobalWhenNoOwned() {
        Secret global = Secret.createGlobal("git.token", "cipher-global");
        when(bindingRepository.findByProjectIdAndAlias(projectId, "git.token"))
                .thenReturn(Optional.empty());
        when(secretRepository.findByProjectIdAndName(projectId, "git.token"))
                .thenReturn(Optional.empty());
        when(secretRepository.findGlobalByName("git.token")).thenReturn(Optional.of(global));
        when(secretCipher.decrypt("cipher-global")).thenReturn("token-global");

        assertEquals(Optional.of("token-global"), useCase.forProject(projectId, "git.token"));
    }

    @Test
    void idForProjectReturnsSecretIdViaSameCascade() {
        Secret global = Secret.createGlobal("proxmox.ssh.private_key", "cipher-key");
        when(bindingRepository.findByProjectIdAndAlias(projectId, "proxmox.ssh.private_key"))
                .thenReturn(Optional.empty());
        when(secretRepository.findByProjectIdAndName(projectId, "proxmox.ssh.private_key"))
                .thenReturn(Optional.empty());
        when(secretRepository.findGlobalByName("proxmox.ssh.private_key")).thenReturn(Optional.of(global));

        assertEquals(Optional.of(global.getId()), useCase.idForProject(projectId, "proxmox.ssh.private_key"));
    }
}
