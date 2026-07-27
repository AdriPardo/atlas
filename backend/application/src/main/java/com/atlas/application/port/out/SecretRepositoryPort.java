package com.atlas.application.port.out;

import com.atlas.domain.secret.Secret;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SecretRepositoryPort {

    Secret save(Secret secret);

    Optional<Secret> findById(UUID id);

    /** Global secret (project_id IS NULL) by name. */
    Optional<Secret> findGlobalByName(String name);

    Optional<Secret> findByProjectIdAndName(UUID projectId, String name);

    boolean existsGlobalByName(String name);

    boolean existsByProjectIdAndName(UUID projectId, String name);

    List<Secret> findAllGlobal();

    List<Secret> findByProjectId(UUID projectId);

    void deleteById(UUID id);

    /** @deprecated use {@link #findGlobalByName(String)} */
    @Deprecated
    default Optional<Secret> findByName(String name) {
        return findGlobalByName(name);
    }

    /** @deprecated use {@link #existsGlobalByName(String)} */
    @Deprecated
    default boolean existsByName(String name) {
        return existsGlobalByName(name);
    }

    /** @deprecated use {@link #findAllGlobal()} */
    @Deprecated
    default List<Secret> findAll() {
        return findAllGlobal();
    }
}
