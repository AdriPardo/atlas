package com.atlas.platform.domain.port.out;

import com.atlas.platform.domain.model.UserAccount;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findById(UUID id);
}
