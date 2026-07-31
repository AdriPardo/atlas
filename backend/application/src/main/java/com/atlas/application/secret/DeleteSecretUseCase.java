package com.atlas.application.secret;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.SecretRepositoryPort;
import com.atlas.domain.secret.Secret;
import com.atlas.domain.shared.ForbiddenException;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Deletes an organization/global secret. ADMIN only. Cascades project bindings (DB FK). */
@Service
@RequiredArgsConstructor
public class DeleteSecretUseCase {

    private final SecretRepositoryPort secretRepository;
    private final ProjectAuthorizationService authorizationService;

    @Transactional
    public void execute(UUID secretId) {
        CurrentUserPort.Actor actor = authorizationService.requireActor();
        if (!actor.isAdmin()) {
            throw new ForbiddenException("Only ADMIN can delete organization secrets");
        }
        Secret secret = secretRepository
                .findById(secretId)
                .orElseThrow(() -> new NotFoundException("Secret not found: " + secretId));
        if (!secret.isGlobal()) {
            throw new NotFoundException("Secret not found: " + secretId);
        }
        secretRepository.deleteById(secretId);
    }
}
