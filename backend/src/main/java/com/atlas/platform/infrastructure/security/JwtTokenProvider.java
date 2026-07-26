package com.atlas.platform.infrastructure.security;

import com.atlas.platform.domain.model.Role;
import com.atlas.platform.domain.model.UserAccount;
import com.atlas.platform.domain.port.out.TokenProviderPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider implements TokenProviderPort {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String createAccessToken(UserAccount user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.expirationMinutes() * 60);
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .claim("installationId", user.getInstallationId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    @Override
    public UUID extractUserId(String token) {
        return UUID.fromString(parse(token).getSubject());
    }

    @Override
    public String extractUsername(String token) {
        return parse(token).get("username", String.class);
    }

    @Override
    public Role extractRole(String token) {
        return Role.valueOf(parse(token).get("role", String.class));
    }

    @Override
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
