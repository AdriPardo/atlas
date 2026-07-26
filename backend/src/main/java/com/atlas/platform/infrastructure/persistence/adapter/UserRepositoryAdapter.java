package com.atlas.platform.infrastructure.persistence.adapter;

import com.atlas.platform.domain.model.UserAccount;
import com.atlas.platform.domain.port.out.UserRepositoryPort;
import com.atlas.platform.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.atlas.platform.infrastructure.persistence.repository.UserJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;
    private final UserPersistenceMapper mapper;

    public UserRepositoryAdapter(UserJpaRepository repository, UserPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return repository.findByUsernameIgnoreCase(username).map(mapper::toDomain);
    }

    @Override
    public Optional<UserAccount> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}
