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
    public Optional<Secret> findByName(String name) {
        return repository.findByNameIgnoreCase(name).map(mapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return repository.existsByNameIgnoreCase(name);
    }

    @Override
    public List<Secret> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }
}
