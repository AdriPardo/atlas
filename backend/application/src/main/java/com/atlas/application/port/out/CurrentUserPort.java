package com.atlas.application.port.out;

import com.atlas.domain.user.Role;
import java.util.Optional;
import java.util.UUID;

public interface CurrentUserPort {

    Optional<Actor> current();

    record Actor(UUID id, String username, Role role) {
        public boolean isAdmin() {
            return role == Role.ADMIN;
        }
    }
}
