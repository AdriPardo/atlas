package com.atlas.infrastructure.security;

import com.atlas.application.auth.AuthenticateFromAuthentikUseCase;
import com.atlas.application.auth.AuthenticateFromAuthentikUseCase.AuthentikIdentity;
import com.atlas.domain.shared.UnauthorizedException;
import com.atlas.domain.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates API requests from Traefik ForwardAuth identity headers when no Bearer JWT is present.
 *
 * <p>Some Traefik Authentik middleware configs strip {@code Authorization} on upstream requests while
 * still injecting {@code X-authentik-*}. Without this filter, {@code /auth/sso} can mint a JWT but
 * {@code /me} returns 403 because the browser token never reaches Spring Security.
 */
@Component
public class AuthentikHeaderAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticateFromAuthentikUseCase authenticateFromAuthentikUseCase;

    @Value("${atlas.security.authentik.enabled:false}")
    private boolean enabled;

    public AuthentikHeaderAuthenticationFilter(
            @Lazy AuthenticateFromAuthentikUseCase authenticateFromAuthentikUseCase) {
        this.authenticateFromAuthentikUseCase = authenticateFromAuthentikUseCase;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        var existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = request.getHeader(AuthentikHeaderNames.USERNAME);
        if (username == null || username.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            User user = authenticateFromAuthentikUseCase.resolveUser(new AuthentikIdentity(
                    username,
                    request.getHeader(AuthentikHeaderNames.GROUPS),
                    request.getHeader(AuthentikHeaderNames.EMAIL),
                    request.getHeader(AuthentikHeaderNames.NAME),
                    request.getHeader(AuthentikHeaderNames.UID)));
            AtlasUserPrincipal principal =
                    new AtlasUserPrincipal(user.getId(), user.getUsername(), user.getRole().name());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (UnauthorizedException ignored) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
