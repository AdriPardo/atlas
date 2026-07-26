package com.atlas.domain.user;

import com.atlas.domain.shared.DomainException;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class User {

    private final UUID id;
    private final String username;
    private final String passwordHash;
    private final Role role;

    private User(UUID id, String username, String passwordHash, Role role) {
        this.id = Objects.requireNonNull(id, "id is required");
        if (username == null || username.isBlank()) {
            throw new DomainException("username is required");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new DomainException("passwordHash is required");
        }
        this.username = username.trim();
        this.passwordHash = passwordHash;
        this.role = Objects.requireNonNull(role, "role is required");
    }

    public static User rehydrate(UUID id, String username, String passwordHash, Role role) {
        return new User(id, username, passwordHash, role);
    }
}
