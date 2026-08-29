package com.atlas.infrastructure.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepositoryPort userRepository;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void usesDatabaseRoleWhenJwtClaimIsStale() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = "jwt";
        when(jwtTokenProvider.getUserId(token)).thenReturn(userId);
        when(jwtTokenProvider.getUsername(token)).thenReturn("apardomo");
        when(jwtTokenProvider.getRole(token)).thenReturn(Role.OPERATOR.name());
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(User.rehydrate(userId, "apardomo", "hash", Role.ADMIN)));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AtlasUserPrincipal principal = (AtlasUserPrincipal) authentication.getPrincipal();
        org.junit.jupiter.api.Assertions.assertEquals(Role.ADMIN.name(), principal.getRole());
        verify(userRepository).findById(userId);
    }

    @Test
    void fallsBackToJwtRoleWhenUserMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = "jwt";
        when(jwtTokenProvider.getUserId(token)).thenReturn(userId);
        when(jwtTokenProvider.getUsername(token)).thenReturn("ghost");
        when(jwtTokenProvider.getRole(token)).thenReturn(Role.OPERATOR.name());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        AtlasUserPrincipal principal =
                (AtlasUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        org.junit.jupiter.api.Assertions.assertEquals(Role.OPERATOR.name(), principal.getRole());
    }
}
