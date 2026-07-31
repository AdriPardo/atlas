package com.atlas.application.secret;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.SecretRepositoryPort;
import com.atlas.domain.secret.Secret;
import com.atlas.domain.shared.ForbiddenException;
import com.atlas.domain.shared.NotFoundException;
import com.atlas.domain.user.Role;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteSecretUseCaseTest {

    @Mock
    private SecretRepositoryPort secretRepository;

    @Mock
    private ProjectAuthorizationService authorizationService;

    private DeleteSecretUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteSecretUseCase(secretRepository, authorizationService);
    }

    @Test
    void deletesGlobalSecretAsAdmin() {
        Secret global = Secret.createGlobal("app.api_key", "cipher");
        when(authorizationService.requireActor())
                .thenReturn(new CurrentUserPort.Actor(UUID.randomUUID(), "admin", Role.ADMIN));
        when(secretRepository.findById(global.getId())).thenReturn(Optional.of(global));

        useCase.execute(global.getId());

        verify(secretRepository).deleteById(global.getId());
    }

    @Test
    void rejectsNonAdmin() {
        when(authorizationService.requireActor())
                .thenReturn(new CurrentUserPort.Actor(UUID.randomUUID(), "ops", Role.OPERATOR));

        assertThrows(ForbiddenException.class, () -> useCase.execute(UUID.randomUUID()));
    }

    @Test
    void rejectsProjectOwnedSecret() {
        UUID projectId = UUID.randomUUID();
        Secret owned = Secret.createForProject(projectId, "x", "c");
        when(authorizationService.requireActor())
                .thenReturn(new CurrentUserPort.Actor(UUID.randomUUID(), "admin", Role.ADMIN));
        when(secretRepository.findById(owned.getId())).thenReturn(Optional.of(owned));

        assertThrows(NotFoundException.class, () -> useCase.execute(owned.getId()));
    }
}
