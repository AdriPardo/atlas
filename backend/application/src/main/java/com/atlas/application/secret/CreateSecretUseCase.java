package com.atlas.application.secret;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.SecretCipherPort;
import com.atlas.application.port.out.SecretRepositoryPort;
import com.atlas.domain.secret.Secret;
import com.atlas.domain.shared.ConflictException;
import com.atlas.domain.shared.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateSecretUseCase {

    private final SecretRepositoryPort secretRepository;
    private final SecretCipherPort secretCipher;
    private final ProjectAuthorizationService authorizationService;

    /** Creates an organization/global secret. ADMIN only. */
    @Transactional
    public Secret execute(CreateSecretCommand command) {
        CurrentUserPort.Actor actor = authorizationService.requireActor();
        if (!actor.isAdmin()) {
            throw new ForbiddenException("Only ADMIN can create organization secrets");
        }
        if (secretRepository.existsGlobalByName(command.name())) {
            throw new ConflictException("Secret name already exists: " + command.name());
        }
        String ciphertext = secretCipher.encrypt(command.value());
        return secretRepository.save(Secret.createGlobal(command.name(), ciphertext));
    }

    public record CreateSecretCommand(String name, String value) {}
}
