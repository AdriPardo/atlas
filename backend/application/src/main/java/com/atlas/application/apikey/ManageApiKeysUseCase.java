package com.atlas.application.apikey;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.ApiKeyRepositoryPort;
import com.atlas.application.port.out.ApiKeyRepositoryPort.Stored;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.domain.shared.ConflictException;
import com.atlas.domain.shared.DomainException;
import com.atlas.domain.shared.ForbiddenException;
import com.atlas.domain.shared.NotFoundException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManageApiKeysUseCase {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApiKeyRepositoryPort apiKeys;
    private final ProjectAuthorizationService authorizationService;

    @Transactional
    public Created create(String name) {
        CurrentUserPort.Actor actor = requireAdmin();
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isBlank() || trimmed.length() > 80) {
            throw new DomainException("API key name must be 1-80 characters");
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String key = "atlas_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String prefix = key.substring(0, 12);
        try {
            Stored stored = apiKeys.insert(
                    actor.id(), trimmed, prefix, ApiKeyHashes.sha256(key), Instant.now());
            return new Created(stored.id(), stored.name(), stored.prefix(), key);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("API key name already exists: " + trimmed);
        }
    }

    @Transactional(readOnly = true)
    public List<Stored> list() {
        CurrentUserPort.Actor actor = requireAdmin();
        return apiKeys.listByUser(actor.id());
    }

    @Transactional
    public void revoke(java.util.UUID id) {
        CurrentUserPort.Actor actor = requireAdmin();
        if (!apiKeys.revoke(id, actor.id(), Instant.now())) {
            throw new NotFoundException("API key not found");
        }
    }

    private CurrentUserPort.Actor requireAdmin() {
        CurrentUserPort.Actor actor = authorizationService.requireActor();
        if (!actor.isAdmin()) {
            throw new ForbiddenException("Only ADMIN can manage API keys");
        }
        return actor;
    }

    public record Created(java.util.UUID id, String name, String prefix, String key) {}
}
