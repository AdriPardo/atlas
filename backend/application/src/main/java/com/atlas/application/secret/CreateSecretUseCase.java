package com.atlas.application.secret;

import com.atlas.application.port.out.SecretCipherPort;
import com.atlas.application.port.out.SecretRepositoryPort;
import com.atlas.domain.secret.Secret;
import com.atlas.domain.shared.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateSecretUseCase {

    private final SecretRepositoryPort secretRepository;
    private final SecretCipherPort secretCipher;

    @Transactional
    public Secret execute(CreateSecretCommand command) {
        if (secretRepository.existsByName(command.name())) {
            throw new ConflictException("Secret name already exists: " + command.name());
        }
        String ciphertext = secretCipher.encrypt(command.value());
        return secretRepository.save(Secret.create(command.name(), ciphertext));
    }

    public record CreateSecretCommand(String name, String value) {}
}
