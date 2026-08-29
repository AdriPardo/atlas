package com.atlas.infrastructure.security;

import static org.mockito.Mockito.when;

import com.atlas.application.auth.AuthenticateFromAuthentikUseCase;
import com.atlas.application.auth.AuthenticateFromAuthentikUseCase.AuthentikIdentity;
import com.atlas.domain.shared.UnauthorizedException;
import com.atlas.domain.user.Role;
import com.atlas.domain.user.User;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthentikHeaderAuthenticationFilterTest {

    @Mock
    private AuthenticateFromAuthentikUseCase authenticateFromAuthentikUseCase;

    @InjectMocks
    private AuthentikHeaderAuthenticationFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesFromForwardAuthHeadersWhenEnabled() throws Exception {
        ReflectionTestUtils.setField(filter, "enabled", true);
        UUID userId = UUID.randomUUID();
        when(authenticateFromAuthentikUseCase.resolveUser(
                        new AuthentikIdentity("apardomo", "Atlas Admins", null, null, null)))
                .thenReturn(User.rehydrate(userId, "apardomo", "hash", Role.ADMIN));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthentikHeaderNames.USERNAME, "apardomo");
        request.addHeader(AuthentikHeaderNames.GROUPS, "Atlas Admins");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AtlasUserPrincipal principal = (AtlasUserPrincipal) authentication.getPrincipal();
        org.junit.jupiter.api.Assertions.assertEquals("apardomo", principal.getUsername());
        org.junit.jupiter.api.Assertions.assertEquals(Role.ADMIN.name(), principal.getRole());
    }

    @Test
    void skipsWhenDisabled() throws Exception {
        ReflectionTestUtils.setField(filter, "enabled", false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthentikHeaderNames.USERNAME, "apardomo");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        org.junit.jupiter.api.Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void skipsWhenUsernameMissing() throws Exception {
        ReflectionTestUtils.setField(filter, "enabled", true);

        filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        org.junit.jupiter.api.Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void ignoresUnauthorizedFromUseCase() throws Exception {
        ReflectionTestUtils.setField(filter, "enabled", true);
        when(authenticateFromAuthentikUseCase.resolveUser(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new UnauthorizedException("Missing Authentik identity"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthentikHeaderNames.USERNAME, "blocked-user");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        org.junit.jupiter.api.Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
