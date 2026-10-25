package com.atlas.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.UserRepositoryPort;
import com.atlas.domain.user.Role;
import com.atlas.domain.user.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SecurityContextCurrentUserAdapterTest {

    @Mock
    private UserRepositoryPort userRepository;

    @InjectMocks
    private SecurityContextCurrentUserAdapter adapter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dbAdminWinsOverStaleOperatorJwt() {
        UUID userId = UUID.randomUUID();
        authenticate(userId, "apardomo", "OPERATOR");
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(User.rehydrate(userId, "apardomo", "sso-hash", Role.ADMIN)));

        Optional<CurrentUserPort.Actor> actor = adapter.current();

        assertTrue(actor.isPresent());
        assertEquals(Role.ADMIN, actor.orElseThrow().role());
        assertTrue(actor.orElseThrow().isAdmin());
        assertEquals(userId, actor.orElseThrow().id());
        assertEquals("apardomo", actor.orElseThrow().username());
    }

    @Test
    void missingRoleClaimStillUsesDbRole() {
        UUID userId = UUID.randomUUID();
        authenticate(userId, "apardomo", null);
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(User.rehydrate(userId, "apardomo", "sso-hash", Role.ADMIN)));

        assertEquals(Role.ADMIN, adapter.current().orElseThrow().role());
    }

    @Test
    void missingUserIsUnauthenticated() {
        UUID userId = UUID.randomUUID();
        authenticate(userId, "ghost", "ADMIN");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertTrue(adapter.current().isEmpty());
    }

    @Test
    void noAuthenticationIsEmpty() {
        SecurityContextHolder.clearContext();

        assertTrue(adapter.current().isEmpty());
    }

    private static void authenticate(UUID userId, String username, String jwtRole) {
        AtlasUserPrincipal principal = new AtlasUserPrincipal(userId, username, jwtRole);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
