package com.atlas.infrastructure.persistence.jpa.adapter;

import com.atlas.application.port.out.SecretRepositoryPort;
import com.atlas.domain.secret.Secret;
import com.atlas.infrastructure.persistence.jpa.mapper.SecretJpaMapper;
import com.atlas.infrastructure.persistence.jpa.repository.SecretJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecretRepositoryAdapter implements SecretRepositoryPort {

    private final SecretJpaRepository repository;
    private final SecretJpaMapper mapper;

    @Override
    public Secret save(Secret secret) {
        return mapper.toDomain(repository.save(mapper.toEntity(secret)));
    }

    @Override
    public Optional<Secret> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Secret> findGlobalByName(String name) {
        return repository.findGlobalByNameIgnoreCase(name).map(mapper::toDomain);
    }

    @Override
    public Optional<Secret> findByProjectIdAndName(UUID projectId, String name) {
        return repository.findByProjectIdAndNameIgnoreCase(projectId, name).map(mapper::toDomain);
    }

    @Override
    public boolean existsGlobalByName(String name) {
        return repository.existsGlobalByNameIgnoreCase(name);
    }

    @Override
    public boolean existsByProjectIdAndName(UUID projectId, String name) {
        return repository.existsByProjectIdAndNameIgnoreCase(projectId, name);
    }

    @Override
    public List<Secret> findAllGlobal() {
        return repository.findAllGlobal().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Secret> findByProjectId(UUID projectId) {
        return repository.findByProjectIdOrderByNameAsc(projectId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
