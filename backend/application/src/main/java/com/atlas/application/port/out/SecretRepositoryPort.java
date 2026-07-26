package com.atlas.application.port.out;

import com.atlas.domain.secret.Secret;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SecretRepositoryPort {

    Secret save(Secret secret);

    Optional<Secret> findById(UUID id);

    Optional<Secret> findByName(String name);

    boolean existsByName(String name);

    List<Secret> findAll();
}
