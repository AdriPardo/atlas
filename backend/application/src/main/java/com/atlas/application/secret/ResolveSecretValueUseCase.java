package com.atlas.application.secret;

import com.atlas.application.port.out.SecretCipherPort;
import com.atlas.application.port.out.SecretRepositoryPort;
import com.atlas.domain.shared.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResolveSecretValueUseCase {

    private final SecretRepositoryPort secretRepository;
    private final SecretCipherPort secretCipher;

    @Transactional(readOnly = true)
    public String byId(UUID id) {
        var secret = secretRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Secret not found: " + id));
        return secretCipher.decrypt(secret.getCiphertext());
    }

    @Transactional(readOnly = true)
    public Optional<String> byName(String name) {
        return secretRepository.findByName(name).map(secret -> secretCipher.decrypt(secret.getCiphertext()));
    }
}
