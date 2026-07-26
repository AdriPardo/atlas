package com.atlas.application.auth;

import com.atlas.application.port.out.PasswordEncoderPort;
import com.atlas.application.port.out.TokenProviderPort;
import com.atlas.application.port.out.UserRepositoryPort;
import com.atlas.domain.shared.UnauthorizedException;
import com.atlas.domain.user.Role;
import com.atlas.domain.user.User;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trusts Authentik ForwardAuth identity headers (injected by Traefik) and mints an Atlas JWT.
 *
 * <p>Security note: headers are only accepted when {@code atlas.security.authentik.enabled=true}.
 * Atlas must not be reachable without Traefik ForwardAuth in that mode; clients can otherwise forge
 * {@code X-authentik-*} headers.
 */
@Service
public class AuthenticateFromAuthentikUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final boolean enabled;
    private final String adminGroup;

    public AuthenticateFromAuthentikUseCase(
            UserRepositoryPort userRepository,
            PasswordEncoderPort passwordEncoder,
            TokenProviderPort tokenProvider,
            @Value("${atlas.security.authentik.enabled:false}") boolean enabled,
            @Value("${atlas.security.authentik.admin-group:Atlas Admins}") String adminGroup) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.enabled = enabled;
        this.adminGroup = adminGroup;
    }

    @Transactional
    public AuthenticationResult execute(AuthentikIdentity identity) {
        if (!enabled) {
            throw new UnauthorizedException("Authentik SSO is not enabled");
        }
        if (identity == null || identity.username() == null || identity.username().isBlank()) {
            throw new UnauthorizedException("Missing Authentik identity");
        }

        String username = identity.username().trim();
        Role role = mapRole(identity.groups());

        User user = userRepository
                .findByUsername(username)
                .map(existing -> syncRole(existing, role))
                .orElseGet(() -> provision(username, role));

        String token = tokenProvider.generateToken(user);
        return new AuthenticationResult(token, "Bearer", tokenProvider.getExpirationSeconds());
    }

    private User syncRole(User existing, Role role) {
        User updated = existing.withRole(role);
        if (updated == existing) {
            return existing;
        }
        return userRepository.save(updated);
    }

    private User provision(String username, Role role) {
        String unusableHash = passwordEncoder.encode("sso-unusable-" + UUID.randomUUID());
        return userRepository.save(User.createSso(username, unusableHash, role));
    }

    /**
     * Maps Authentik groups to Atlas roles.
     *
     * <ul>
     *   <li>Any group whose name contains the configured admin group (default {@code Atlas Admins},
     *       case-insensitive) → {@link Role#ADMIN}
     *   <li>Otherwise → {@link Role#OPERATOR}
     * </ul>
     *
     * <p>Authentik sends groups as a pipe-separated list in {@code X-authentik-groups}.
     */
    Role mapRole(String groupsHeader) {
        List<String> groups = parseGroups(groupsHeader);
        String needle = adminGroup == null ? "atlas admins" : adminGroup.trim().toLowerCase(Locale.ROOT);
        boolean isAdmin = groups.stream().anyMatch(g -> g.toLowerCase(Locale.ROOT).contains(needle));
        return isAdmin ? Role.ADMIN : Role.OPERATOR;
    }

    static List<String> parseGroups(String groupsHeader) {
        if (groupsHeader == null || groupsHeader.isBlank()) {
            return List.of();
        }
        return Arrays.stream(groupsHeader.split("[|,]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public record AuthentikIdentity(String username, String groups, String email, String name, String uid) {}
}
