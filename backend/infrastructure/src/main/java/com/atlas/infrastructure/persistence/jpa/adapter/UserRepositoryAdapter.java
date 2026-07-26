package com.atlas.infrastructure.persistence.jpa.adapter;

import com.atlas.application.port.out.UserRepositoryPort;
import com.atlas.domain.user.User;
import com.atlas.infrastructure.persistence.jpa.mapper.UserJpaMapper;
import com.atlas.infrastructure.persistence.jpa.repository.UserJpaRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;
    private final UserJpaMapper mapper;

    @Override
    public Optional<User> findByUsername(String username) {
        return repository.findByUsernameIgnoreCase(username).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}
