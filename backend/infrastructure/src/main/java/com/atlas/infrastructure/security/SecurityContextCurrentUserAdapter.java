package com.atlas.infrastructure.security;

import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.UserRepositoryPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityContextCurrentUserAdapter implements CurrentUserPort {

    private final UserRepositoryPort userRepository;

    @Override
    public Optional<Actor> current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AtlasUserPrincipal principal)) {
            return Optional.empty();
        }
        // Role lives on the user row. /me already reads it; a JWT minted before a role
        // change (or with a missing claim) must not downgrade an org ADMIN to OPERATOR.
        return userRepository
                .findById(principal.getId())
                .map(user -> new Actor(user.getId(), user.getUsername(), user.getRole()));
    }
}
