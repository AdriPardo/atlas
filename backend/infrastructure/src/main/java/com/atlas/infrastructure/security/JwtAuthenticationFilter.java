package com.atlas.infrastructure.security;

import com.atlas.application.apikey.ApiKeyHashes;
import com.atlas.application.port.out.ApiKeyRepositoryPort;
import com.atlas.application.port.out.UserRepositoryPort;
import com.atlas.domain.user.User;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-Api-Key";
    private static final String API_KEY_PREFIX = "atlas_";

    private final JwtTokenProvider jwtTokenProvider;
    private final ApiKeyRepositoryPort apiKeyRepository;
    private final UserRepositoryPort userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if ((apiKey == null || apiKey.isBlank()) && header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            if (token.startsWith(API_KEY_PREFIX)) {
                apiKey = token;
            } else {
                authenticateJwt(request, token);
            }
        }
        if (apiKey != null && !apiKey.isBlank()) {
            authenticateApiKey(request, apiKey.trim());
        }
        filterChain.doFilter(request, response);
    }

    private void authenticateJwt(HttpServletRequest request, String token) {
        try {
            UUID userId = jwtTokenProvider.getUserId(token);
            String username = jwtTokenProvider.getUsername(token);
            String role = jwtTokenProvider.getRole(token);
            setAuthentication(request, new AtlasUserPrincipal(userId, username, role));
        } catch (JwtException | IllegalArgumentException ignored) {
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticateApiKey(HttpServletRequest request, String apiKey) {
        if (!apiKey.startsWith(API_KEY_PREFIX)) {
            SecurityContextHolder.clearContext();
            return;
        }
        String hash = ApiKeyHashes.sha256(apiKey);
        var userId = apiKeyRepository.findActiveUserIdByHash(hash);
        if (userId.isEmpty()) {
            SecurityContextHolder.clearContext();
            return;
        }
        userRepository
                .findById(userId.get())
                .ifPresentOrElse(
                        user -> {
                            apiKeyRepository.touchLastUsed(hash, Instant.now());
                            setAuthentication(request, principal(user));
                        },
                        SecurityContextHolder::clearContext);
    }

    private static AtlasUserPrincipal principal(User user) {
        return new AtlasUserPrincipal(user.getId(), user.getUsername(), user.getRole().name());
    }

    private static void setAuthentication(HttpServletRequest request, AtlasUserPrincipal principal) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
