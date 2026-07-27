package com.atlas.application.secret;

import com.atlas.application.port.out.ProjectSecretBindingRepositoryPort;
import com.atlas.application.port.out.SecretCipherPort;
import com.atlas.application.port.out.SecretRepositoryPort;
import com.atlas.domain.secret.ProjectSecretBinding;
import com.atlas.domain.secret.Secret;
import com.atlas.domain.shared.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decrypts secrets. Deploy/Git resolve names with {@link #forProject(UUID, String)} in order:
 *
 * <ol>
 *   <li>project binding alias
 *   <li>project-owned secret name
 *   <li>organization/global secret name
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class ResolveSecretValueUseCase {

    private final SecretRepositoryPort secretRepository;
    private final ProjectSecretBindingRepositoryPort bindingRepository;
    private final SecretCipherPort secretCipher;

    @Transactional(readOnly = true)
    public String byId(UUID id) {
        var secret = secretRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Secret not found: " + id));
        return secretCipher.decrypt(secret.getCiphertext());
    }

    /** Organization/global secret by name only. Prefer {@link #forProject} for deploy. */
    @Transactional(readOnly = true)
    public Optional<String> byName(String name) {
        return secretRepository.findGlobalByName(name).map(this::decrypt);
    }

    /**
     * Resolve a logical name (e.g. {@code git.token}) for a project using binding → owned → global.
     */
    @Transactional(readOnly = true)
    public Optional<String> forProject(UUID projectId, String name) {
        if (projectId == null || name == null || name.isBlank()) {
            return Optional.empty();
        }
        Optional<ProjectSecretBinding> binding = bindingRepository.findByProjectIdAndAlias(projectId, name);
        if (binding.isPresent()) {
            return secretRepository.findById(binding.get().getSecretId()).map(this::decrypt);
        }
        Optional<Secret> owned = secretRepository.findByProjectIdAndName(projectId, name);
        if (owned.isPresent()) {
            return owned.map(this::decrypt);
        }
        return secretRepository.findGlobalByName(name).map(this::decrypt);
    }

    private String decrypt(Secret secret) {
        return secretCipher.decrypt(secret.getCiphertext());
    }
}
