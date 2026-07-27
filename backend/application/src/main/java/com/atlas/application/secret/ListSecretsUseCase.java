package com.atlas.application.secret;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.SecretRepositoryPort;
import com.atlas.domain.secret.Secret;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListSecretsUseCase {

    private final SecretRepositoryPort secretRepository;
    private final ProjectAuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public List<Secret> execute() {
        authorizationService.requireActor();
        return secretRepository.findAll();
    }
}
