package com.atlas.application.port.out;

import com.atlas.domain.user.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

    Optional<User> findByUsername(String username);

    Optional<User> findById(UUID id);
}
