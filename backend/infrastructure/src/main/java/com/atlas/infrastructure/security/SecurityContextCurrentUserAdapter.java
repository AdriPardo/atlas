package com.atlas.infrastructure.security;

import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.domain.user.Role;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextCurrentUserAdapter implements CurrentUserPort {

    @Override
    public Optional<Actor> current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AtlasUserPrincipal principal)) {
            return Optional.empty();
        }
        Role role;
        try {
            role = Role.valueOf(principal.getRole());
        } catch (Exception ex) {
            role = Role.OPERATOR;
        }
        return Optional.of(new Actor(principal.getId(), principal.getUsername(), role));
    }
}
